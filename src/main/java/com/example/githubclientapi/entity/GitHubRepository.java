package com.example.githubclientapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true, value = {"fork"})
public record GitHubRepository(String name, Owner owner, boolean fork, List<Branch> branches) {
}



