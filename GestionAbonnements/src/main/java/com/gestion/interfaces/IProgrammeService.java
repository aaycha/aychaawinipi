package com.gestion.interfaces;
import com.gestion.entities.Programme;


import java.sql.SQLException;
import java.util.List;

public interface IProgrammeService extends IService<Programme> {
    List<Programme> getByEventId(int eventId) throws SQLException;

    void add(Programme p) throws SQLException;

    void update(Programme p) throws SQLException;

    void delete(int idProg) throws SQLException;

    List<Programme> getAll() throws SQLException;
}
