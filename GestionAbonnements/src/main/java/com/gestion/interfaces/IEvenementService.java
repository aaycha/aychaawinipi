package com.gestion.interfaces;

import com.gestion.entities.Evenement;

import java.sql.SQLException;
import java.util.List;

public interface IEvenementService extends IService<Evenement> {

    // English aliases to match IProgrammeService and user controllers
    void add(Evenement e) throws SQLException;

    void update(Evenement e) throws SQLException;

    void delete(int id) throws SQLException;

    List<Evenement> getAll() throws SQLException;

    // Streams (recherche/filtre/tri) sur une liste
    List<Evenement> rechercher(List<Evenement> events, String keyword);

    List<Evenement> filtrerParType(List<Evenement> events, String type);

    List<Evenement> filtrerParLieu(List<Evenement> events, String keyword);

    List<Evenement> trierParDateAsc(List<Evenement> events);

    List<Evenement> trierParDateDesc(List<Evenement> events);

    Evenement getById(int id) throws SQLException;
}
