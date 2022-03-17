package ru.jcups.task.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.RequestTemplate;
import feign.Response;
import feign.form.FormData;
import feign.form.MultipartFormContentProcessor;
import feign.form.multipart.FormDataWriter;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import ru.jcups.task.feign.SumSubClient;
import ru.jcups.task.model.Applicant;
import ru.jcups.task.model.DocType;
import ru.jcups.task.model.HttpMethod;
import ru.jcups.task.model.Metadata;
import ru.jcups.task.util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static ru.jcups.task.util.Util.SUMSUB_RESOURCE_PATH;
import static ru.jcups.task.util.Util.SUMSUB_TEST_BASE_URL;

public class OpenFeignSumSubService implements SumSubService {

	private final MultipartFormContentProcessor processor;
	private final SumSubClient client;
	private final ObjectMapper mapper = Util.mapper;

	public OpenFeignSumSubService() {
		this.processor = new MultipartFormContentProcessor(new JacksonEncoder());
		this.processor.addFirstWriter(new FormDataWriter());
		this.client = Feign.builder()
				.encoder(new JacksonEncoder(mapper))
				.decoder(new JacksonDecoder(mapper))
				.target(SumSubClient.class, SUMSUB_TEST_BASE_URL);
	}

	@Override
	public boolean setPending(String applicantId) {
		String path = SUMSUB_RESOURCE_PATH +"/" + applicantId + "/status/pending";
		return client.setPending(applicantId, getSecurityHeaders(path, HttpMethod.POST, null)).status() == 200;
	}

	@Override
	public String getStatus(String applicantId) {
		String path = SUMSUB_RESOURCE_PATH +"/" + applicantId + "/status";
		return client.getStatus(applicantId, getSecurityHeaders(path, HttpMethod.GET, null)).getReviewStatus();
	}

	@Override
	public String getInfo(String applicantId) {
		String path = SUMSUB_RESOURCE_PATH +"/" + applicantId + "/one";

		Response response = client.getInfo(applicantId, getSecurityHeaders(path, HttpMethod.GET, null));
		try (BufferedReader reader = new BufferedReader(response.body().asReader(StandardCharsets.UTF_8))) {
			StringBuilder s = new StringBuilder();
			while (reader.ready())
				s.append(reader.readLine()).append("\n");
			Applicant applicant = mapper.readValue(s.toString(), Applicant.class);
			return applicant.getInfo().getFirstNameEn() + " " + applicant.getInfo().getLastNameEn();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String addDocument(String applicantId, File file) {
		String path = SUMSUB_RESOURCE_PATH +"/" + applicantId + "/info/idDoc";
		Metadata metadata = new Metadata(DocType.PASSPORT, "RUS");
		try {
			String metadataString = mapper.writeValueAsString(metadata);

			Map<String, Object> formDataMap = new LinkedHashMap<>();
			formDataMap.put("metadata", metadataString);
			FormData formData = FormData.builder().data(Files.readAllBytes(file.toPath()))
					.contentType("image/*").fileName(file.getName()).build();
			formDataMap.put("content", formData);

			Map<String, Collection<String>> headers = getSecurityHeaders(path, HttpMethod.POST,
					readMultipartAsBytes(formDataMap));
//			headers.put("X-Return-Doc-Warnings", Collections.singleton("true"));
			Response response = client.addDocument(metadataString, formData,
					headers, applicantId);
			try {
				return response.headers().get("x-image-id").toString()
						.replaceAll("[\\[\\]]", "");
			} catch (NullPointerException e) {
// 				Он может сработать примерно с 50ой попытки если использовать рекурсию
//				return addDocument(applicantId, file);
				return Util.addDocument(applicantId, file);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String createApplicant(String externalUserId, String levelName) {
		Applicant applicant = new Applicant(externalUserId);
		String path = SUMSUB_RESOURCE_PATH +"?levelName=" + levelName;
		try {
			Map<String, String> params = new LinkedHashMap<>();
			params.put("levelName", levelName);
			byte[] body = mapper.writeValueAsBytes(applicant);
			return client.create(params, applicant,
					getSecurityHeaders(path, HttpMethod.POST, body)).getId();
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private byte[] readMultipartAsBytes(Map<String, Object> data) {
		RequestTemplate template = new RequestTemplate();
		processor.process(template, StandardCharsets.UTF_8, data);
		return template.body();
	}

	private Map<String, Collection<String>> getSecurityHeaders(String path, HttpMethod method, byte[] body) {
		Map<String, Collection<String>> headers = new LinkedHashMap<>();
		Util.getSecurityHeaders(path, method, body)
				.forEach((s, s2) -> headers.put(s, Collections.singleton(s2)));
		return headers;
	}
}
