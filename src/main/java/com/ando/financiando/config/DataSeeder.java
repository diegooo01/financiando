package com.ando.financiando.config;

import com.ando.financiando.model.Category;
import com.ando.financiando.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataSeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }

        List<Category> defaults = List.of(
                new Category("Comida", "🍽️"),
                new Category("Transporte", "🚌"),
                new Category("Entretenimiento", "🎮"),
                new Category("Servicios", "💡"),
                new Category("Salud", "💊"),
                new Category("Otros", "📦")
        );

        categoryRepository.saveAll(defaults);
    }
}