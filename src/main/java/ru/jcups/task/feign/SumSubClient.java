package ru.jcups.task.feign;

import feign.*;
import feign.form.FormData;
import ru.jcups.task.model.Applicant;
import ru.jcups.task.model.Status;

import java.util.Collection;
import java.util.Map;

public interface SumSubClient {

	@Headers("Content-Type: application/json; charset=UTF-8")
	@RequestLine("POST /resources/applicants")
	Applicant create(
			@QueryMap Map<String, String> queryParams,
			Applicant applicant,
			@HeaderMap Map<String, Collection<String>> headers);

	@Headers("Content-Type: multipart/form-data; charset=utf-8")
	@RequestLine("POST /resources/applicants/{applicantId}/info/idDoc")
	Response addDocument(
			@Param("metadata") String metadata,
			@Param("content") FormData file,
			@HeaderMap Map<String, Collection<String>> securityHeaders,
			@Param("applicantId") String applicantId);

	@RequestLine("POST /resources/applicants/{applicantId}/status/pending")
	Response setPending(@Param("applicantId") String applicantId,
						@HeaderMap Map<String, Collection<String>> headers);

	@RequestLine("GET /resources/applicants/{applicantId}/status")
	Status getStatus(
			@Param("applicantId") String applicantId,
			@HeaderMap Map<String, Collection<String>> headers);

	@RequestLine("GET /resources/applicants/{applicantId}/one")
	Response getInfo(
			@Param("applicantId") String applicantId,
			@HeaderMap Map<String, Collection<String>> headers);
}
