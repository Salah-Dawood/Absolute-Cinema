package com.cinemasaar.recommendationservice.exception;

// Catalogue failure must never produce invented or stale movie IDs.
public class CatalogueUnavailableException extends RuntimeException {}
