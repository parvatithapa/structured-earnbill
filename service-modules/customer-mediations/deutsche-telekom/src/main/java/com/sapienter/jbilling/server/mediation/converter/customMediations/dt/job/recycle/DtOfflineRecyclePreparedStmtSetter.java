package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.recycle;

import org.springframework.jdbc.core.PreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DtOfflineRecyclePreparedStmtSetter implements PreparedStatementSetter {

    private Integer mediationCfgId;

    @Override
    public void setValues(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setInt(1, mediationCfgId);
    }

    public void setMediationCfgId(Integer mediationCfgId) {
        this.mediationCfgId = mediationCfgId;
    }
}
