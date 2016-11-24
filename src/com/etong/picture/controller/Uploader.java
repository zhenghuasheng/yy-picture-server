/**
 * Created by wunan on 2014/9/26.
 * Copyright (c) 2014, Etong. All rights reserved.
 */
package com.etong.picture.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Controller
public class Uploader {

    @RequestMapping(value = "/upload", method = {RequestMethod.POST, RequestMethod.GET})
    public ModelAndView uploadRequest(HttpServletRequest req, HttpServletResponse res) {
        String redirect = req.getParameter("result");
        // 验证上传内容了类型
        String contentType = req.getContentType();

        if (!contentType.contains("multipart/form-data")) {
            try {
                res.sendError(400);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

            return null;
        }

        int maxFileSize = 5000 * 1024;
        int maxMemSize = 5000 * 1024;
        DiskFileItemFactory factory = new DiskFileItemFactory();

        // 设置内存中存储文件的最大值
        factory.setSizeThreshold(maxMemSize);

        // 本地存储的数据大于 maxMemSize.
        factory.setRepository(new File("/home/wunan/temp"));

        // 创建一个新的文件上传处理程序
        ServletFileUpload upload = new ServletFileUpload(factory);
        // 设置最大上传的文件大小
        upload.setSizeMax(maxFileSize);
        ServletContext context = req.getServletContext();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        String date = simpleDateFormat.format(new Date().getTime());
        String reqDir = req.getParameter("dir");
        String webPath = "";

        try {
            webPath = context.getResource("/").getPath();
            webPath = context.getRealPath("/")+"/";
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
        }

        String filePath = context.getInitParameter("upload_path")
                 + "/" + ((reqDir == null) ? "" : reqDir)+ "/" + date;

        if (filePath.charAt(filePath.length() - 1) != '/') {
            filePath += "/";
        }

        List fileItems = null;

        try {
            fileItems = upload.parseRequest(req);
        } catch (FileUploadException e) {
            System.out.println(e.getMessage());
            return null;
        }

        Iterator fileIter = fileItems.iterator();
        String uploadUrl = context.getInitParameter("server_url");
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        jsonObject.put("files", jsonArray);

        while (fileIter.hasNext()) {
            FileItem fileItem = (FileItem) fileIter.next();

            if (fileItem.isFormField()) {
                continue;
            }
            String imagefileName = fileItem.getName();
            String fileExt = imagefileName.substring(imagefileName.lastIndexOf(".") + 1).toLowerCase();

            JSONObject result = new JSONObject();
            jsonArray.add(result);
            result.put("name", fileItem.getName());
            String fileName = UUID.randomUUID().toString()+"."+fileExt;

            if (saveFile(fileItem, webPath + filePath, fileName)) {
                result.put("error", 0);
                result.put("url", uploadUrl + filePath + fileName);
            } else {
                result.put("error", 1);
            }
        }


        res.setHeader("Access-Control-Allow-Origin", "*");
        responseResult(jsonObject.toJSONString(), redirect, res);
        return null;
    }

    @RequestMapping(value = "/file_manager_json", method = {RequestMethod.POST, RequestMethod.GET})
    public ModelAndView file_manager_json(HttpServletRequest req, HttpServletResponse res){
        ServletContext context = req.getServletContext();
//根目录路径，可以指定绝对路径，比如 /var/www/attached/
        String rootPath = context.getRealPath("/") + context.getInitParameter("upload_path");
//根目录URL，可以指定绝对路径，比如 http://www.yoursite.com/attached/
        String rootUrl  = context.getContextPath() + context.getInitParameter("upload_path");
//图片扩展名
        String[] fileTypes = new String[]{"gif", "jpg", "jpeg", "png", "bmp"};

        String dirName = req.getParameter("dir");
        if (dirName != null) {
//            if(!Arrays.<String>asList(new String[]{"image", "flash", "media", "file"}).contains(dirName)){
//                //out.println("Invalid Directory name.");
//                return null;
//            }
            rootPath += dirName + "/";
            rootUrl += dirName + "/";
            File saveDirFile = new File(rootPath);
            if (!saveDirFile.exists()) {
                saveDirFile.mkdirs();
            }
        }
//根据path参数，设置各路径和URL
        String path = req.getParameter("path") != null ? req.getParameter("path") : "";
        String currentPath = rootPath + path;
        String currentUrl = rootUrl + path;
        String currentDirPath = path;
        String moveupDirPath = "";
        if (!"".equals(path)) {
            String str = currentDirPath.substring(0, currentDirPath.length() - 1);
            moveupDirPath = str.lastIndexOf("/") >= 0 ? str.substring(0, str.lastIndexOf("/") + 1) : "";
        }

//排序形式，name or size or type
        String order = req.getParameter("order") != null ? req.getParameter("order").toLowerCase() : "name";

//不允许使用..移动到上一级目录
        if (path.indexOf("..") >= 0) {
            //out.println("Access is not allowed.");
            responseResult("Access is not allowed.", null, res);
            return null;
        }
//最后一个字符不是/
        if (!"".equals(path) && !path.endsWith("/")) {
            //out.println("Parameter is not valid.");
            responseResult("Parameter is not valid.", null, res);
            return null;
        }
//目录不存在或不是目录
        File currentPathFile = new File(currentPath);
        if(!currentPathFile.isDirectory()){
            //out.println("Directory does not exist.");
            responseResult("Directory does not exist.", null, res);
            return null;
        }

//遍历目录取的文件信息
        List<Hashtable> fileList = new ArrayList<Hashtable>();
        if(currentPathFile.listFiles() != null) {
            for (File file : currentPathFile.listFiles()) {
                Hashtable<String, Object> hash = new Hashtable<String, Object>();
                String fileName = file.getName();
                if(file.isDirectory()) {
                    hash.put("is_dir", true);
                    hash.put("has_file", (file.listFiles() != null));
                    hash.put("filesize", 0L);
                    hash.put("is_photo", false);
                    hash.put("filetype", "");
                } else if(file.isFile()){
                    String fileExt = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                    hash.put("is_dir", false);
                    hash.put("has_file", false);
                    hash.put("filesize", file.length());
                    hash.put("is_photo", Arrays.<String>asList(fileTypes).contains(fileExt));
                    hash.put("filetype", fileExt);
                }
                hash.put("filename", fileName);
                hash.put("datetime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(file.lastModified()));
                fileList.add(hash);
            }
        }

        if ("size".equals(order)) {
            Collections.sort(fileList, new SizeComparator());
        } else if ("type".equals(order)) {
            Collections.sort(fileList, new TypeComparator());
        } else {
            Collections.sort(fileList, new NameComparator());
        }

        String uploadUrl = context.getInitParameter("server_url");
        JSONObject result = new JSONObject();
        result.put("moveup_dir_path", moveupDirPath);
        result.put("current_dir_path",currentDirPath+"/");
        result.put("current_url",uploadUrl+ currentUrl);
        result.put("total_count", fileList.size());
        result.put("file_list", fileList);

        res.setContentType("application/json; charset=UTF-8");
        System.out.println(result.toJSONString());
        //out.println(result.toJSONString());
        res.setHeader("Access-Control-Allow-Origin", "*");
        responseResult(result.toJSONString(), null, res);
        return null;
    }

    private boolean saveFile(FileItem fileItem
            , String filePath, String fileName) {
        File file = new File(filePath, fileName);

        if (!file.exists()) {
            new File(filePath).mkdirs();
        }

        try {
            if (!file.createNewFile()) {
                return false;
            }

            fileItem.write(file);
            return true;
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return false;
    }

    private void responseResult(String result, String redirect, HttpServletResponse res) {
        try {
            if ((redirect == null) || redirect.isEmpty()) {
                res.setContentType("application/json;charset=utf-8");
                res.getWriter().print(result);
            } else {
                res.setContentType("text/html;charset=utf-8");

                if (redirect.equalsIgnoreCase("html")) {
                    res.getWriter().print(result);
                } else {
                    res.sendRedirect(redirect + "?" + result);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private class NameComparator implements Comparator {
        public int compare(Object a, Object b) {
            Hashtable hashA = (Hashtable)a;
            Hashtable hashB = (Hashtable)b;
            if (((Boolean)hashA.get("is_dir")) && !((Boolean)hashB.get("is_dir"))) {
                return -1;
            } else if (!((Boolean)hashA.get("is_dir")) && ((Boolean)hashB.get("is_dir"))) {
                return 1;
            } else {
                return ((String)hashA.get("filename")).compareTo((String)hashB.get("filename"));
            }
        }
    }
    private class SizeComparator implements Comparator {
        public int compare(Object a, Object b) {
            Hashtable hashA = (Hashtable)a;
            Hashtable hashB = (Hashtable)b;
            if (((Boolean)hashA.get("is_dir")) && !((Boolean)hashB.get("is_dir"))) {
                return -1;
            } else if (!((Boolean)hashA.get("is_dir")) && ((Boolean)hashB.get("is_dir"))) {
                return 1;
            } else {
                if (((Long)hashA.get("filesize")) > ((Long)hashB.get("filesize"))) {
                    return 1;
                } else if (((Long)hashA.get("filesize")) < ((Long)hashB.get("filesize"))) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }
    }
    private class TypeComparator implements Comparator {
        public int compare(Object a, Object b) {
            Hashtable hashA = (Hashtable)a;
            Hashtable hashB = (Hashtable)b;
            if (((Boolean)hashA.get("is_dir")) && !((Boolean)hashB.get("is_dir"))) {
                return -1;
            } else if (!((Boolean)hashA.get("is_dir")) && ((Boolean)hashB.get("is_dir"))) {
                return 1;
            } else {
                return ((String)hashA.get("filetype")).compareTo((String)hashB.get("filetype"));
            }
        }
    }
}
