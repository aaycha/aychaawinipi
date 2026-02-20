package com.gestion;

import com.gestion.entities.Repas;
import com.gestion.services.RepasServiceImpl;
import java.util.List;

public class DbChecker {
    public static void main(String[] args) {
        RepasServiceImpl service = new RepasServiceImpl();
        List<Repas> list = service.findAll();
        System.out.println("Total items: " + list.size());
        for (Repas r : list) {
            System.out.println("ID: " + r.getId() + " | Nom: " + r.getNom() + " | ImageUrl: " + r.getImageUrl());
        }
    }
}
