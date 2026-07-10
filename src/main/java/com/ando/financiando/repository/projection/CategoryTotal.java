package com.ando.financiando.repository.projection;

import java.math.BigDecimal;

public interface CategoryTotal {
    String getCategoryName();
    String getEmoji();
    BigDecimal getTotal();
}