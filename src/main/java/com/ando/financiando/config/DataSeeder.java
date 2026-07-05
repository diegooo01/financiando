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
                new Category("Comida", "🍽️", List.of(
                        "almuerzo", "comida", "menu", "menú", "cena", "desayuno",
                        "restaurante", "café", "cafe", "snack", "lonche")),
                new Category("Transporte", "🚌", List.of(
                        "taxi", "uber", "bus", "combi", "metro", "metropolitano",
                        "pasaje", "gasolina", "grifo", "transporte")),
                new Category("Entretenimiento", "🎮", List.of(
                        "cine", "pelicula", "película", "juego", "netflix", "spotify",
                        "concierto", "salida", "fiesta", "trago", "cerveza")),
                new Category("Servicios", "💡", List.of(
                        "luz", "agua", "internet", "celular", "recibo", "alquiler",
                        "servicio", "telefono", "teléfono")),
                new Category("Salud", "💊", List.of(
                        "farmacia", "medicina", "doctor", "clinica", "clínica",
                        "gym", "gimnasio", "consulta")),
                new Category("Otros", "📦", List.of())
        );

        categoryRepository.saveAll(defaults);
    }
}