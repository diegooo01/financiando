package com.ando.financiando.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 8)
    private String emoji;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "category_keywords",
            joinColumns = @JoinColumn(name = "category_id")
    )
    @Column(name = "keyword")
    private List<String> keywords = new ArrayList<>();

    protected Category() {
    }

    public Category(String name, String emoji, List<String> keywords) {
        this.name = name;
        this.emoji = emoji;
        this.keywords = keywords;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public List<String> getKeywords() { return keywords; }

    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
}