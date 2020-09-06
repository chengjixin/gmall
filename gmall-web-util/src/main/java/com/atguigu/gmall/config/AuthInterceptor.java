package com.atguigu.gmall.config;

import com.alibaba.fastjson.JSON;
import com.atuigu.gmall.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    //进入控制器之前执行
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getParameter("newToken");
        //登录成功之https://www.jd.com/?newToken=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0.3Z6Lwc4nZ3FDIOx-SEkd_hiImKqCrB-reiYFB6X8RBo
        if (token != null){
            //将token保存到cookie中
            CookieUtil.setCookie(request,response,"token",token,WebConst.COOKIE_MAXAGE,false);
        }

        //登录成功后继续操作启动的web模块，此时的token是不共享的，但是可以通过cookie 获取token信息
        if (token == null){
            token = CookieUtil.getCookieValue(request, "token", false);
        }

        //将用户名从token中解密出来，保存到域对象中，用于页面的渲染
        if (token != null){
            //token解密，通过得到的map集合，拿到用户NickName
            Map map = getUserMapByToken(token);
            String nickName = (String) map.get("nickName");

            //将用户姓名存放到域对象中
            request.setAttribute("nickName",nickName);
        }

        // 获取用户访问的控制器上是否有  注解 @LoginRequire
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        //获取方法上的注解
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);

        if (methodAnnotation != null){
            String salt = request.getHeader("X-forwarded-for");
            // 远程调用！
            // http://passport.atguigu.com/verify?token=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0.3Z6Lwc4nZ3FDIOx-SEkd_hiImKqCrB-reiYFB6X8RBo&salt=192.168.67.1
            // http://passport.atguigu.com/verify?token=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0.t0197PHxR5uHNmt5Ipk9DDFC7iXiH2b4Hi9E8Uv6nBI&salt=192.168.92.1
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&salt=" + salt);

            if ("success".equals(result)){
                //认证成功
                Map map = getUserMapByToken(token);
                String userId = (String) map.get("userId");

                // 保存到作用域
                request.setAttribute("userId",userId);
                // 放行！
                return true;
            }else{
                //认证失败，通过注解的属性值判断是否属需要登录的页面
                if(methodAnnotation.autoRedirect()){
                    //表示需要登录才能访问的页面
                    // 应该跳转到登录页面！http://item.gmall.com/37.html -----> http://passport.atguigu.com/index?originUrl=http%3A%2F%2Fitem.gmall.com%2F37.html
                    // 得到用户访问的url 路径
                    String requestURL = request.getRequestURL().toString();
                    System.out.println("转码前的ulr：" + requestURL);

                    // 将 http://item.gmall.com/37.html 编码 http%3A%2F%2Fitem.gmall.com%2F37.html
                    String encodeURL = URLEncoder.encode(requestURL,"UTF-8");
                    System.out.println("转码后的url：" + encodeURL);

                    response.sendRedirect(WebConst.LOGIN_ADDRESS + "?originUrl=" + encodeURL);

                    // 拦截！
                    return false;
                }else {
                    String requestURL = request.getRequestURL().toString();
                    System.out.println("转码前的ulr：" + requestURL);

                    // 将 http://item.gmall.com/37.html 编码 http%3A%2F%2Fitem.gmall.com%2F37.html
                    String encodeURL = URLEncoder.encode(requestURL,"UTF-8");
                    System.out.println("转码后的url：" + encodeURL);

                    //将转码后的参数列表保存到域对象中
                    request.setAttribute("orginUrl",encodeURL);
                }
            }
        }


        return true;
    }

    //token 解密
    private Map getUserMapByToken(String token) {
        //newToken=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0.3Z6Lwc4nZ3FDIOx-SEkd_hiImKqCrB-reiYFB6X8RBo
        //token信息一共分为三部分
        String tokenUserInfo = StringUtils.substringBetween(token, ".");
        System.out.println(tokenUserInfo);

        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        //解密 因为本身就是通过base64UrlCode 方式加密，所以解密反其道而行
        byte[] decode = base64UrlCodec.decode(tokenUserInfo);
        // byte ---> String ---> Map (因为不能直接转换，所以需要先转换成String类型)
        String tokenJson = new String(decode);
        return JSON.parseObject(tokenJson,Map.class);
    }

    //进入控制器之后，返回视图之前执行
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        super.postHandle(request, response, handler, modelAndView);
    }

    //视图渲染之后执行
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        super.afterCompletion(request, response, handler, ex);
    }
}
