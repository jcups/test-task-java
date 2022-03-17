package ru.jcups.task.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okio.Buffer;
import org.apache.commons.codec.binary.Hex;
import ru.jcups.task.model.DocType;
import ru.jcups.task.model.HttpMethod;
import ru.jcups.task.model.Metadata;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class Util {
	public static final ObjectMapper mapper = new ObjectMapper();
	public static final String SUMSUB_TEST_BASE_URL = "https://test-api.sumsub.com";
	public static final String SUMSUB_RESOURCE_PATH = "/resources/applicants";

	public static String SUMSUB_SECRET_KEY = "YOUR_SECRET_KEY";
	public static String SUMSUB_APP_TOKEN = "YOUR_APP_TOKEN";

	public static String createSignature(long ts, HttpMethod httpMethod, String path, byte[] body) {
		try {
			Mac hmacSha256 = Mac.getInstance("HmacSHA256");
			hmacSha256.init(new SecretKeySpec(SUMSUB_SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			hmacSha256.update((ts + httpMethod.name() + path).getBytes(StandardCharsets.UTF_8));
			byte[] bytes = body == null ? hmacSha256.doFinal() : hmacSha256.doFinal(body);
			return Hex.encodeHexString(bytes);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new RuntimeException(e);
		}
	}

	public static String addDocument(String applicantId, File document) {
		String path = SUMSUB_RESOURCE_PATH + "/" + applicantId + "/info/idDoc";
		Metadata metadata = new Metadata(DocType.PASSPORT, "RUS");
		try {
			RequestBody body = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("metadata", mapper.writeValueAsString(metadata))
					.addFormDataPart("content", document.getName(),
							RequestBody.create(document, MediaType.parse("image/*"))).build();
			Request request = new Request.Builder()
					.url(SUMSUB_TEST_BASE_URL + path)
					.headers(Headers.of(getSecurityHeaders(path, HttpMethod.POST, readBodyAsBytes(body))))
					.post(body).build();
			Response response = new OkHttpClient().newCall(request).execute();
			return response.header("X-Image-Id");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Map<String, String> getSecurityHeaders(String path, HttpMethod method, byte[] body) {
		Map<String, String> headers = new HashMap<>();
		headers.put("X-App-Token", SUMSUB_APP_TOKEN);
		long ts = Instant.now().getEpochSecond();
		headers.put("X-App-Access-Sig", createSignature(ts, method, path, body));
		headers.put("X-App-Access-Ts", String.valueOf(ts));
		return headers;
	}

	private static byte[] readBodyAsBytes(RequestBody body) throws IOException {
		Buffer buffer = new Buffer();
		body.writeTo(buffer);
		return buffer.readByteArray();
	}

	public static void setSumsubAppToken(String sumsubAppToken) {
		SUMSUB_APP_TOKEN = sumsubAppToken;
	}

	public static void setSumsubSecretKey(String sumsubSecretKey) {
		SUMSUB_SECRET_KEY = sumsubSecretKey;
	}
}
