﻿/*
 * Copyright [2016] [zhangsong <songm.cn>].
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package songm.im.backstage.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import songm.im.backstage.HttpConnException;
import songm.im.backstage.HttpResult;

public class HttpUtil {

    private static final String APPKEY = "SM-Server-Key";
    private static final String NONCE = "SM-Nonce";
    private static final String TIMESTAMP = "SM-Timestamp";
    private static final String SIGNATURE = "SM-Signature";

    private static SSLContext sslCtx = null;
    static {
        try {
            sslCtx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs,
                        String string) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] xcs,
                        String string) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            sslCtx.init(null, new TrustManager[] { tm }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

            @Override
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }

        });

        HttpsURLConnection
                .setDefaultSSLSocketFactory(sslCtx.getSocketFactory());
    }

    public static HttpURLConnection createHttpConnection(String appKey,
            String appSecret, String url) throws HttpConnException {
        try {
            return _createHttpConnection(appKey, appSecret, url);
        } catch (MalformedURLException e) {
            throw new HttpConnException(e);
        } catch (ProtocolException e) {
            throw new HttpConnException(e);
        } catch (IOException e) {
            throw new HttpConnException(e);
        }
    }

    /**
     * 添加签名header
     * 
     * @param appKey
     * @param appSecret
     * @param url
     * @return
     * @throws MalformedURLException
     * @throws IOException
     * @throws ProtocolException
     */
    private static HttpURLConnection _createHttpConnection(String appKey,
            String appSecret, String url)
            throws MalformedURLException, IOException, ProtocolException {
        String nonce = String.valueOf(Math.random() * 1000000);
        String timestamp = String.valueOf(System.currentTimeMillis());
        StringBuilder toSign = new StringBuilder(appSecret).append(nonce)
                .append(timestamp);
        String sign = CodeUtils.sha1(toSign.toString());

        URL uRL = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) uRL.openConnection();
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(30000);

        conn.setRequestProperty(APPKEY, appKey);
        conn.setRequestProperty(NONCE, nonce);
        conn.setRequestProperty(TIMESTAMP, timestamp);
        conn.setRequestProperty(SIGNATURE, sign);
        conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");

        return conn;
    }

    public static void setBodyParameter(StringBuilder sb,
            HttpURLConnection conn) throws HttpConnException {
        if (conn.getRequestMethod().equals("GET")) {
            return;
        }
        try {
            _setBodyParameter(sb, conn);
        } catch (IOException e) {
            throw new HttpConnException(e);
        }
    }

    private static void _setBodyParameter(StringBuilder sb,
            HttpURLConnection conn) throws IOException {
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        out.writeBytes(sb.toString());
        out.flush();
        out.close();
    }

    public static byte[] readInputStream(InputStream inStream)
            throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        byte[] data = outStream.toByteArray();
        outStream.close();
        inStream.close();
        return data;
    }

    public static String returnResult(HttpURLConnection conn)
            throws HttpConnException {
        HttpResult r;
        try {
            r = _returnResult(conn);
        } catch (IOException e) {
            throw new HttpConnException(e);
        }
        if (r.getHttpCode() != 200) {
            throw new HttpConnException(r.getResult());
        }
        return r.getResult();
    }

    private static HttpResult _returnResult(HttpURLConnection conn)
            throws IOException {
        InputStream input = null;
        if (conn.getResponseCode() == 200) {
            input = conn.getInputStream();
        } else {
            input = conn.getErrorStream();
        }
        return new HttpResult(conn.getResponseCode(),
                new String(readInputStream(input)));
    }

    public static void setConnection(String key, String value,
            HttpURLConnection conn) throws HttpConnException {
        if (key.equals("method")) {
            try {
                conn.setRequestMethod(value);
            } catch (ProtocolException e) {
                throw new HttpConnException(e);
            }
        } else if (key.equals("caches")) {
            conn.setUseCaches(Boolean.parseBoolean(value));
        } else if (key.equals("timeout")) {
            conn.setConnectTimeout(Integer.parseInt(value));
        } else {
            throw new HttpConnException("HTTP conn options '"+key+"' error");
        }
    }
}
