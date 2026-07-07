package com.ando.financiando.model;

import jakarta.persistence.*;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type = TransactionType.EXPENSE;

    protected Category() {
    }

    public Category(String name, String emoji, TransactionType type, List<String> keywords) {
        this.name = name;
        this.emoji = emoji;
        this.type = type;
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

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }
}