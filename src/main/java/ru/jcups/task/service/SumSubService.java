package ru.jcups.task.service;

import java.io.File;

public interface SumSubService {

	String createApplicant(String externalUserId, String levelName);

	String addDocument(String applicantId, File document);

	String getInfo(String applicantId);

	String getStatus(String applicantId);

	boolean setPending(String applicantId);
}
