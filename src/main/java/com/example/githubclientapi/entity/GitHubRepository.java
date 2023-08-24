package com.example.githubclientapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepository(String name, Owner owner, List<Branch> branches) {
}



