package com.tm.tsm_atelier.common.service;

public interface EmailService {

	void sendVerificationEmail(String to, String firstName, String verificationLink);
}
