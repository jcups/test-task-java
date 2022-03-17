package ru.jcups.task.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import ru.jcups.task.model.Applicant;
import ru.jcups.task.model.HttpMethod;
import ru.jcups.task.model.Status;
import ru.jcups.task.util.Util;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;

import static ru.jcups.task.util.Util.SUMSUB_RESOURCE_PATH;
import static ru.jcups.task.util.Util.SUMSUB_TEST_BASE_URL;

public class JerseySumSubService implements SumSubService {

	private final ObjectMapper mapper = Util.mapper;
	private final Client client = ClientBuilder.newClient(new ClientConfig()
			.register(MultiPartFeature.class));
	private final WebTarget target = client.target(SUMSUB_TEST_BASE_URL);

	@Override
	public String getInfo(String applicantId) {
		String path = SUMSUB_RESOURCE_PATH + "/" + applicantId + "/one";
		Response response = target.path(path).request()
				.headers(getSecurityHeaders(path, HttpMethod.GET, null)).get();
		Applicant applicant = response.readEntity(Applicant.class);
		return applicant.getInfo().getFirstNameEn() + " " + applicant.getInfo().getLastNameEn();
	}

	@Override
	public boolean setPending(String applicantId) {
		String path = SUMSUB_RESOURCE_PATH + "/" + applicantId + "/status/pending";
		return target.path(path).request()
				.headers(getSecurityHeaders(path, HttpMethod.POST, null))
				.post(null).getStatus() == 200;
	}

	@Override
	public String getStatus(String applicantId) {
		String path = SUMSUB_RESOURCE_PATH + "/" + applicantId + "/status";
		Response response = target.path(path).request()
				.headers(getSecurityHeaders(path, HttpMethod.GET, null)).get();
		if (response.hasEntity())
			return response.readEntity(Status.class).getReviewStatus();
		else
			throw new RuntimeException("Response of 'status' does not contains body");
	}

	@Override
	public String addDocument(String applicantId, File document) {
		return Util.addDocument(applicantId, document);
	}

	@Override
	public String createApplicant(String externalUserId, String levelName) {
		String path = SUMSUB_RESOURCE_PATH;
		Applicant applicant = new Applicant(externalUserId);
		try {
			Response response = target.path(path).queryParam("levelName", levelName)
					.request(MediaType.APPLICATION_JSON)
					.headers(getSecurityHeaders(path + "?levelName=" + levelName,
							HttpMethod.POST, mapper.writeValueAsBytes(applicant)))
					.post(Entity.entity(applicant, MediaType.APPLICATION_JSON_TYPE));

			if (response.getStatus() != 201)
				throw new RuntimeException("Response code: " + response.getStatus());
			else
				return mapper.readValue(response.readEntity(String.class), Applicant.class).getId();
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private MultivaluedMap<String, Object> getSecurityHeaders(String path, HttpMethod method, byte[] body) {
		MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
		Util.getSecurityHeaders(path, method, body)
				.forEach((s, s2) -> headers.put(s, new LinkedList<>(Collections.singleton(s2))));
		return headers;
	}

}
