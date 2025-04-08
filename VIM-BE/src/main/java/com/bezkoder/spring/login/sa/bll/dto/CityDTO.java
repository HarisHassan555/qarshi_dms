package com.bezkoder.spring.login.sa.bll.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * DTO for {@link com.bezkoder.spring.login.sa.dal.entities.CfgTblCity}
 */
public class CityDTO implements Serializable {

    private final String txtCityCode;
    private final String txtCityName;

    public CityDTO(String txtCityCode, String txtCityName) {

        this.txtCityCode = txtCityCode;
        this.txtCityName = txtCityName;
    }


    public String getTxtCityCode() {
        return txtCityCode;
    }

    public String getTxtCityName() {
        return txtCityName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CityDTO entity = (CityDTO) o;
        return
                Objects.equals(this.txtCityCode, entity.txtCityCode) &&
                Objects.equals(this.txtCityName, entity.txtCityName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(txtCityCode, txtCityName);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "txtCityCode = " + txtCityCode + ", " +
                "txtCityName = " + txtCityName + ")";
    }
}