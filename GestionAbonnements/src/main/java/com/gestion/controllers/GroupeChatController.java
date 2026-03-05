package com.gestion.controllers;

import com.gestion.entities.GroupeChat;
import com.gestion.services.GroupeChatService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@WebServlet("/groupes")
public class GroupeChatController extends HttpServlet {

    private GroupeChatService service;

    @Override
    public void init() {
        service = new GroupeChatService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String action = Optional.ofNullable(req.getParameter("action"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElse("list");

        switch (action) {

            case "delete":
                parseInt(req.getParameter("id"))
                        .ifPresent(service::supprimer);

                resp.sendRedirect(req.getContextPath() + "/groupes");
                break;

            case "edit":
                parseInt(req.getParameter("id"))
                        .map(service::getById)
                        .ifPresent(g -> req.setAttribute("groupe", g));

                // liste groupes
                List<GroupeChat> groupes = service.afficher();
                req.setAttribute("groupes", groupes);

                req.getRequestDispatcher("/views/groupes.jsp").forward(req, resp);
                break;

            default:
                List<GroupeChat> groupesList = service.afficher();
                req.setAttribute("groupes", groupesList);
                req.getRequestDispatcher("/views/groupes.jsp").forward(req, resp);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String nom = Optional.ofNullable(req.getParameter("nom")).orElse("").trim();
        String description = Optional.ofNullable(req.getParameter("description")).orElse("").trim();
        String type = Optional.ofNullable(req.getParameter("type")).orElse("PUBLIC").trim();

        // id optionnel => update si présent sinon create
        Optional<Integer> idOpt = parseInt(req.getParameter("id"));

        idOpt.ifPresentOrElse(
                id -> {
                    GroupeChat g = new GroupeChat();
                    g.setId(id);
                    g.setNom(nom);
                    g.setDescription(description);
                    g.setType(type);
                    service.modifier(g);
                },
                () -> {
                    // petite validation Stream: éviter nom vide
                    boolean ok = Stream.of(nom)
                            .map(String::trim)
                            .anyMatch(s -> !s.isBlank());

                    if (ok) {
                        service.ajouter(new GroupeChat(nom, description, type, 1));
                    }
                });

        resp.sendRedirect(req.getContextPath() + "/groupes");
    }

    // ===== Helpers (sans autre fichier) =====
    private Optional<Integer> parseInt(String value) {
        try {
            return Optional.ofNullable(value)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(Integer::parseInt);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}