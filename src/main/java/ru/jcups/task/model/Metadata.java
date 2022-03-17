package ru.jcups.task.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Metadata {

	DocType idDocType;
	String country;
	String idDocSubType;
	String firstName;
	String middleName;
	String lastName;
	String issuedDate;
	String validUntilNo;
	String number;
	String dob;
	String placeOfBirth;

	public Metadata(DocType idDocType, String country) {
		this.idDocType = idDocType;
		this.country = country;
	}
}
