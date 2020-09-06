package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.*;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.apache.catalina.Host;
import org.apache.lucene.util.QueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.highlight.Highlighter;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ListServiceImpl implements ListService {

    @Autowired  //调用es客户端的对象
    private JestClient jestClient;

    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 商品上架
     * @param skuLsInfo
     */
    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {
        /*
            准备执行的动作
            执行
         */
        Index build = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();

            try {
            DocumentResult execute = jestClient.execute(build);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        SearchResult searchResult = null;
        //定义好dsl 语句
        String query = makeQueryStringForSearch(skuLsParams);
        //准备好需要执行的动作
        Search build = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        try {
            //执行
            searchResult = jestClient.execute(build);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (searchResult != null){
            //因为es返回的结果集为Json字符串，我们需要制作结果集
            SkuLsResult skuLsResult = makeResultForSearch(searchResult,skuLsParams);
            return skuLsResult;
        }
        return null;
    }

    @Override
    public void incrHotScore(String skuId) {
        /*
            需求：刷新es中的热点数据，es操作会导致io，从而经常性的刷新es会导致性能降低
                这里使用redis，记录我们访问了多少次商品详情页面，每次访问，hotScore +1

            使用redis中的数据类型为：zset zincrby
         */
        Jedis jedis = redisUtil.getJedis();

        //记录次数，每次+1,使用zset
        Double hotScore = jedis.zincrby("hotScore", 1, "skuId:" + skuId);

        if (hotScore % 10 == 0){
            //修改es中的hotScore
            updateHotScore(skuId,Math.round(hotScore));
        }

    }
    //修改es中的hotScore
    private void updateHotScore(String skuId, long hotScore) {
        /*
            1.编写dsl语句
            2.确定动作
            3.执行语句
         */
        String updateDSL = "{\n" +
                "  \"doc\": {\n" +
                "      \"hotScore\": "+hotScore+"\n" +
                "  }\n" +
                "}";

        Update build = new Update.Builder(updateDSL).index(ES_INDEX).type(ES_TYPE).id(skuId).build();

        try {
            jestClient.execute(build);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    //制作返回的结果集
    private SkuLsResult makeResultForSearch(SearchResult searchResult, SkuLsParams skuLsParams) {
        SkuLsResult skuLsResult = new SkuLsResult();
        //初始化skuLsResult对象
        //1.保存商品的！ skuLsInfoList
        //创建一个skuLsinfos集合，用于封装skuLsInfo数据
        ArrayList<SkuLsInfo> skuLsInfos = new ArrayList<>();
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        if (hits != null && hits.size() > 0){
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                SkuLsInfo skuLsInfo = hit.source;

                //表示用户是通过全文检索进入的，skuName是需要高亮显示的
                if (hit.highlight != null && hit.highlight.size() > 0){
                    Map<String, List<String>> list = hit.highlight;
                    List<String> highlight = list.get("skuName");
                    String SkuNameStr = highlight.get(0);

                    //覆盖原有的skuName
                    skuLsInfo.setSkuName(SkuNameStr);
                }
                skuLsInfos.add(skuLsInfo);
            }
        }
        //将商品数据集合存放到结果集中
        skuLsResult.setSkuLsInfoList(skuLsInfos);

        //2.总条数
        skuLsResult.setTotal(searchResult.getTotal());

        //3.总页数
        long totalPages = (searchResult.getTotal() + skuLsParams.getPageSize() - 1) / skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPages);

        //4.得到集合（valueId）在聚合模块中包含
        //创建一个集合用于封装valueId
        ArrayList<String> stringArrayList = new ArrayList<>();
        TermsAggregation termsAggregation = searchResult.getAggregations().getTermsAggregation("groupby_attr");
        List<TermsAggregation.Entry> buckets = termsAggregation.getBuckets();
        if (buckets != null && buckets.size() > 0){
            for (TermsAggregation.Entry bucket : buckets) {
                String valueId = bucket.getKey();
                stringArrayList.add(valueId);
            }
        }
        //将valueId的集合存放到结果集当中
        skuLsResult.setAttrValueIdList(stringArrayList);

        return skuLsResult;
    }

    //根据用户输入的检索条件生成dsl 语句
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        //定义查询器 类似于 {  }
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //相当于{ query：{bool：{}}}
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //判断是否按照三级分类Id过滤
        if(skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() >0){
            // filter --- term {"term": {"catalog3Id": "61"}}
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //判断是否按照平台属性过滤
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length >0){
            // 循环  {"term": {"skuAttrValueList.valueId": "82"}}
            for (String valueId : skuLsParams.getValueId()) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        //判断是否按照全文检索查询（keyword）
        if (skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0){
            /*
                {"match": {
                  "skuName": "小米三代"
                }}
             */
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            boolQueryBuilder.must(matchQueryBuilder);

            //因为只有是按照全文检索（keyword）查询，才能有高亮效果，所以高亮设置需要在全文检索的条件下

            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags("<span style=color:red>");   //自定义高亮显示标签，前缀
            highlightBuilder.postTags("</span>");                 //自定义高亮显示标签，后缀
            highlightBuilder.field("skuName");                    //需要高亮显示的字段

            searchSourceBuilder.highlight(highlightBuilder);
        }

        //按照hotScore 进行排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        //分页 from = pageSize * (PageNo - 1)
        //这里默认 当前页码：1   每页显示条数：20
        int from = skuLsParams.getPageSize() * (skuLsParams.getPageNo() - 1);  //表示开始的的下标
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(skuLsParams.getPageSize());

        //聚合
         /*
        "aggs": {
            "groupby_attr": {
              "terms": {
                "field": "skuAttrValueList.valueId"
              }
            }
          }
         */
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId.keyword");
        searchSourceBuilder.aggregation(groupby_attr);

        //类似于 { query：{}}
        searchSourceBuilder.query(boolQueryBuilder);
        String query = searchSourceBuilder.toString();

        System.out.println("query:" + query);
        return query;
    }
}
