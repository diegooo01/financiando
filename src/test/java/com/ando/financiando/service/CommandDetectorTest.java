package com.ando.financiando.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandDetectorTest {

    private CommandDetector detector;

    @BeforeEach
    void setUp() {
        detector = new CommandDetector();
    }

    @Test
    void detectaComandoBalanceSimple() {
        assertThat(detector.isBalanceQuery("balance")).isTrue();
    }

    @Test
    void detectaFraseNatural() {
        assertThat(detector.isBalanceQuery("como voy este mes")).isTrue();
    }

    @Test
    void detectaFraseConAcentos() {
        assertThat(detector.isBalanceQuery("cuánto gasté")).isTrue();
    }

    @Test
    void detectaFraseConTextoAlrededor() {
        assertThat(detector.isBalanceQuery("oye y cuanto llevo gastado?")).isTrue();
    }

    @Test
    void ignoraMayusculas() {
        assertThat(detector.isBalanceQuery("MIS FINANZAS")).isTrue();
    }

    @Test
    void noDetectaUnGastoNormal() {
        assertThat(detector.isBalanceQuery("almuerzo 20")).isFalse();
    }

    @Test
    void noDetectaMensajeVacio() {
        assertThat(detector.isBalanceQuery("   ")).isFalse();
    }

    @Test
    void noDetectaNull() {
        assertThat(detector.isBalanceQuery(null)).isFalse();
    }
}