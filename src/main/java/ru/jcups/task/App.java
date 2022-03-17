package ru.jcups.task;

import ru.jcups.task.service.JerseySumSubService;
import ru.jcups.task.service.OpenFeignSumSubService;
import ru.jcups.task.service.SumSubService;
import ru.jcups.task.util.Util;

import java.io.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;

public class App {

	private static SumSubService service;

	public static void main(String[] args) {
		init(args.length == 0 ? new String[]{"0"}: args);
		String id = UUID.randomUUID().toString();
		String levelName = "basic-kyc-level";
		File document = new File("./src/main/resources/images/rus_passport_from_web.jpg");
		try {
			process(id, levelName, document);
		} catch (Exception e) {
			System.out.println("Exception: " + e.getClass().getSimpleName() + " with message: " + e.getMessage());
		}
	}

	private static void process(String id, String levelName, File document) throws InterruptedException {
		String applicantId = service.createApplicant(id, levelName);
		System.out.println("applicantId = " + applicantId);

		String imageId = service.addDocument(applicantId, document);
		System.out.println("imageId = " + imageId);

		String status = service.getStatus(applicantId);
		System.out.println("status = " + status);

		if (service.setPending(applicantId)) {
			status = service.getStatus(applicantId);
			System.out.println("status = " + status);

			if (status.equals("pending")) {
				do {
					Thread.sleep(500);

					status = service.getStatus(applicantId);
					System.out.println("status = " + status);
				} while (!status.equals("completed"));

				String info = service.getInfo(applicantId);
				System.out.println("info = " + info);
			}
		}
	}

	private static void init(String[] args) {
		if (args[0].equals("0"))
			service = new JerseySumSubService();
		else if (args[0].equals("1"))
			service = new OpenFeignSumSubService();
		Properties properties = new Properties();
		File file = new File("./src/main/resources/config/app.properties");
		try {
			properties.load(new FileInputStream(file));
			if (properties.size() == 0)
				throw new IOException();
			String sumsub_app_token = properties.getProperty("SUMSUB_APP_TOKEN");
			Util.setSumsubAppToken(sumsub_app_token);
			String sumsub_secret_key = properties.getProperty("SUMSUB_SECRET_KEY");
			Util.setSumsubSecretKey(sumsub_secret_key);
		} catch (IOException e) {
			System.out.println("Loading error, please input data again: \nYour app token: ");
			Scanner scanner = new Scanner(System.in);
			String line = scanner.next();
			Util.setSumsubAppToken(line);
			properties.setProperty("SUMSUB_APP_TOKEN", line);
			System.out.println("Your secret key: ");
			line = scanner.next();
			Util.setSumsubSecretKey(line);
			properties.setProperty("SUMSUB_SECRET_KEY", line);
			try {
				properties.store(new OutputStreamWriter(new FileOutputStream(file)), "");
			} catch (IOException ex) {
				System.out.println("Store error");
			}
		}
	}
}
