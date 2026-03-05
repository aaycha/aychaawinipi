package com.gestion.controllers;

import com.gestion.entities.GroupeChat;
import com.gestion.entities.MessageChat;
import com.gestion.services.GroupeChatService;
import com.gestion.services.MessageChatService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@WebServlet("/messages")
public class MessageChatController extends HttpServlet {

    private MessageChatService messageService;
    private GroupeChatService groupeService;

    @Override
    public void init() {
        messageService = new MessageChatService();
        groupeService = new GroupeChatService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // idGroupe obligatoire
        int idGroupe = parseInt(req.getParameter("idGroupe")).orElse(-1);
        if (idGroupe <= 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "idGroupe est obligatoire");
            return;
        }

        String action = Optional.ofNullable(req.getParameter("action"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElse("show");

        switch (action) {

            case "delete":
                parseInt(req.getParameter("id"))
                        .ifPresent(messageService::supprimer);

                resp.sendRedirect(req.getContextPath() + "/messages?idGroupe=" + idGroupe);
                break;

            case "edit":
                // charger groupe + messages
                GroupeChat groupe = groupeService.getById(idGroupe);
                List<MessageChat> messages = messageService.afficherParGroupe(idGroupe);

                req.setAttribute("groupe", groupe);
                req.setAttribute("messages", messages);

                // message à éditer
                parseInt(req.getParameter("id"))
                        .map(messageService::getById)
                        .ifPresent(msg -> req.setAttribute("msgToEdit", msg));

                req.getRequestDispatcher("/views/messages.jsp").forward(req, resp);
                break;

            default:
                GroupeChat groupeDefault = groupeService.getById(idGroupe);
                List<MessageChat> messagesDefault = messageService.afficherParGroupe(idGroupe);

                req.setAttribute("groupe", groupeDefault);
                req.setAttribute("messages", messagesDefault);

                req.getRequestDispatcher("/views/messages.jsp").forward(req, resp);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        int idGroupe = parseInt(req.getParameter("idGroupe")).orElse(-1);
        if (idGroupe <= 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "idGroupe est obligatoire");
            return;
        }

        String contenu = Optional.ofNullable(req.getParameter("contenu"))
                .orElse("")
                .trim();

        Optional<Integer> idMsgOpt = parseInt(req.getParameter("id"));

        // Stream validation: contenu non vide
        boolean contenuValide = Stream.of(contenu)
                .map(String::trim)
                .anyMatch(s -> !s.isBlank());

        if (!contenuValide) {
            resp.sendRedirect(req.getContextPath() + "/messages?idGroupe=" + idGroupe);
            return;
        }

        // update si id existe, sinon create
        idMsgOpt.ifPresentOrElse(
                id -> {
                    MessageChat m = new MessageChat();
                    m.setId(id);
                    m.setContenu(contenu);
                    messageService.modifier(m);
                },
                () -> messageService.ajouter(new MessageChat(contenu, idGroupe, 1)));

        resp.sendRedirect(req.getContextPath() + "/messages?idGroupe=" + idGroupe);
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