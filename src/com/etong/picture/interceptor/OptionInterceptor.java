package com.etong.picture.interceptor;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * Created by Administrator on 2015/9/24.
 */
public class OptionInterceptor implements HandlerInterceptor{

    private Logger logger=Logger.getLogger(OptionInterceptor.class);
    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        String requestMethod=httpServletRequest.getMethod().toString();
        if (requestMethod.equals(RequestMethod.OPTIONS.name())){
            logger.debug("接口：" + httpServletRequest.getRequestURL() + "---OPTIONS请求方法被调用");

            ControlleRassist.resposeOption(httpServletResponse);

        }else {
            ControlleRassist.responseHelper(httpServletResponse);
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {
        if (e!=null){
            logger.info(e.getMessage()+"--->"+new Date().toLocaleString());
        }
    }
}
