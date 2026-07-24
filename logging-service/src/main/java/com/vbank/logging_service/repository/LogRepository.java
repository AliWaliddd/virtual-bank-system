package com.vbank.logging_service.repository;

import com.vbank.logging_service.entity.LogEntry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LogRepository extends JpaRepository<LogEntry, UUID> {
}