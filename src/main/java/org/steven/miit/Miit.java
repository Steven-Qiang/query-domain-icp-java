package org.steven.miit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import okhttp3.*;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.net.URL;
import java.util.Base64;


public class Miit {
    private static final boolean IS_DEBUG = false;
    private static final String OPENCV_LIBRARY_NAME = "opencv_java452.dll";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36";
    public static final String BASE_URL = "https://hlwicpfwc.miit.gov.cn/icpproject_query/api";
    public static final String REFERER_URL = "https://beian.miit.gov.cn/";
    public static final String REQUEST_AUTH_DATA = "authKey=39ed33127a1680b60e057bdb8d127772&timeStamp=1657782828830";

    private AuthTokenResponseBean authToken = null;
    private SignToken signToken = null;

    public QueryResponse queryByUnitName(String unitName) throws Exception {
        return queryByUnitName(unitName, 1, 40);
    }

    public QueryResponse queryByUnitName(String unitName, int pageNum, int pageSize) throws Exception {
        authToken = getAuthToken();
        signToken = getSignToken();

        JsonObject jsonObject = request(
                "/icpAbbreviateInfo/queryByCondition",
                "{\"pageNum\":\"" + pageNum + "\",\"pageSize\":\"" + pageSize + "\",\"unitName\":\"" + unitName + "\"}",
                "application/json"
        );

        Gson gson = new Gson();
        JsonObject params = jsonObject.get("params").getAsJsonObject();
        if (!jsonObject.get("code").getAsString().equals("200")) {
            throw new IllegalStateException("查询失败 " + jsonObject.get("msg").getAsString());
        }

        return gson.fromJson(params, QueryResponse.class);

    }

    public SignToken getSignToken() throws Exception {
        short retryTimes = 0;
        while (retryTimes < 3) {
            VerifyImage verifyImage = getVerifyImage();
            int puzzleResult = puzzleVerifyImage(verifyImage);
            JsonObject resp = request(
                    "/image/checkImage",
                    "{\"key\":\"" + verifyImage.getUuid() + "\",\"value\":\"" + puzzleResult + "\"}",
                    "application/json"
            );
            if (!resp.get("code").getAsString().equals("200")) {
                retryTimes++;
                if (IS_DEBUG) System.out.println("第" + retryTimes + "次重试破解验证码");
                continue;
            }
            String token = resp.get("params").getAsString();
            SignToken signToken = new SignToken(token, verifyImage.getUuid());
            if (IS_DEBUG) System.out.println("signToken=" + signToken);
            return signToken;
        }
        throw new IllegalStateException("破解验证码失败");
    }

    public int puzzleVerifyImage(VerifyImage verifyImage) {
        Mat result = new Mat();
        Imgproc.matchTemplate(verifyImage.bigImage, verifyImage.smallImage, result, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult minMaxLocResult = Core.minMaxLoc(result);
        int x = (int) minMaxLocResult.maxLoc.x;
        if (IS_DEBUG) System.out.println("验证码位置: " + x);
        return x;
    }

    public AuthTokenResponseBean getAuthToken() throws Exception {
        JsonObject resp = request("/auth", REQUEST_AUTH_DATA, null);
        if (!resp.get("code").getAsString().equals("200")) {
            throw new IllegalStateException("获取AuthToken失败 " + resp.get("msg").getAsString());
        }
        Gson gson = new Gson();
        return gson.fromJson(resp.get("params"), AuthTokenResponseBean.class);
    }

    public VerifyImage getVerifyImage() throws Exception {
        JsonObject resp = request("/image/getCheckImage", REQUEST_AUTH_DATA, null);
        if (!resp.get("code").getAsString().equals("200")) {
            throw new IllegalStateException("获取验证码图片失败 " + resp.get("msg").getAsString());
        }
        JsonObject params = resp.get("params").getAsJsonObject();
        String smallImage = params.get("smallImage").getAsString(),
                bigImage = params.get("bigImage").getAsString(),
                uuid = params.get("uuid").getAsString();
        int height = params.get("height").getAsInt();
        return new VerifyImage(smallImage, bigImage, uuid, height);
    }

    public JsonObject request(String path, String data, String type) throws Exception {
        if (type == null) type = "application/x-www-form-urlencoded";
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        MediaType mediaType = MediaType.parse(type);
        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + path)
                .method("POST", RequestBody.create(mediaType, data))
                .addHeader("Referer", REFERER_URL)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Content-Type", type);
        if (authToken != null) {
            builder.addHeader("token", authToken.getBussiness());
        }
        if (signToken != null) {
            builder.addHeader("sign", signToken.getToken());
            builder.addHeader("uuid", signToken.getUuid());
        }
        Request request = builder.build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) throw new IllegalStateException("response body is null");
            return JsonParser.parseString(body.string()).getAsJsonObject();
        }
    }

    @NoArgsConstructor
    @Data
    public static class AuthTokenResponseBean {
        private int expire;
        private String refresh;
        private String bussiness;
    }

    @Data
    @AllArgsConstructor
    public static class SignToken {
        private String token;
        private String uuid;
    }

    @Data
    public static class VerifyImage {
        private Mat smallImage;
        private Mat bigImage;
        private String uuid;
        private int height;

        public VerifyImage(String s_smallImage, String s_bigImage, String uuid, int height) {
            this.uuid = uuid;
            this.height = height;
            this.smallImage = base64ToMat(s_smallImage);
            this.bigImage = base64ToMat(s_bigImage);
        }

    }

    public static Mat base64ToMat(String s_image) {
        byte[] bytes = Base64.getDecoder().decode(s_image);
        return Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_UNCHANGED);
    }

    static {
        URL url = ClassLoader.getSystemResource("lib/" + OPENCV_LIBRARY_NAME);
        try {
            System.load(url.getPath());
        } catch (UnsatisfiedLinkError unsatisfiedLinkError) {
            File fileOut = new File(OPENCV_LIBRARY_NAME);
            if (!fileOut.exists()) {
                try (
                        BufferedInputStream fis = new BufferedInputStream(url.openStream());
                        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(fileOut))
                ) {
                    byte[] buf = new byte[4096];
                    int i;
                    while ((i = fis.read(buf)) != -1) {
                        fos.write(buf, 0, i);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            System.load(fileOut.getAbsolutePath());
        }
    }
}