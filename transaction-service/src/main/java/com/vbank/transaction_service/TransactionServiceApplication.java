package com.vbank.transaction_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TransactionServiceApplication {

	public static void main(String[] args) {

		SpringApplication.run(TransactionServiceApplication.class, args);
		System.out.println("hello from txn service");
	}

}
