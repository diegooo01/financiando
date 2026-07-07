package com.ando.financiando.config;

import com.ando.financiando.model.Category;
import com.ando.financiando.model.TransactionType;
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
                // --- Categorías de GASTO ---
                new Category("Comida", "🍽️", TransactionType.EXPENSE, List.of(
                        "almuerzo", "comida", "menu", "menú", "cena", "desayuno",
                        "restaurante", "café", "cafe", "snack", "lonche")),
                new Category("Transporte", "🚌", TransactionType.EXPENSE, List.of(
                        "taxi", "uber", "bus", "combi", "metro", "metropolitano",
                        "pasaje", "gasolina", "grifo", "transporte")),
                new Category("Entretenimiento", "🎮", TransactionType.EXPENSE, List.of(
                        "cine", "pelicula", "película", "juego", "netflix", "spotify",
                        "concierto", "salida", "fiesta", "trago", "cerveza", "tono", "tonear")),
                new Category("Servicios", "💡", TransactionType.EXPENSE, List.of(
                        "luz", "agua", "internet", "celular", "recibo", "alquiler",
                        "servicio", "telefono", "teléfono")),
                new Category("Salud", "💊", TransactionType.EXPENSE, List.of(
                        "farmacia", "medicina", "doctor", "clinica", "clínica",
                        "gym", "gimnasio", "consulta")),
                new Category("Otros", "📦", TransactionType.EXPENSE, List.of()),

                // --- Categorías de INGRESO ---
                new Category("Sueldo", "💵", TransactionType.INCOME, List.of(
                        "sueldo", "salario", "pago", "quincena")),
                new Category("Ventas", "🏷️", TransactionType.INCOME, List.of(
                        "venta", "vendi", "vendí")),
                new Category("Otros ingresos", "📥", TransactionType.INCOME, List.of())
        );

        categoryRepository.saveAll(defaults);
    }
}