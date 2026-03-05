package com.gestion.services;

import com.gestion.entities.equipementwael;
import com.gestion.entities.GroupeChat;
import com.gestion.entities.MessageChat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Statistiques dynamiques pour le dashboard (groupes les plus actifs,
 * équipements les plus vus).
 */
public class StatistiqueService {

    private final MessageChatService messageService;
    private final GroupeChatService groupeService;
    private final equipemementservicewael equipementService;

    public StatistiqueService() {
        this.messageService = new MessageChatService();
        this.groupeService = new GroupeChatService();
        this.equipementService = new equipemementservicewael();
    }

    /**
     * Retourne le nombre de messages par groupe (id_groupe -> count).
     * Pour PieChart "groupe le plus actif".
     */
    public Map<Integer, Long> getMessagesCountByGroupe() {
        return messageService.afficher().stream()
                .filter(m -> m.getIdGroupe() > 0)
                .collect(Collectors.groupingBy(MessageChat::getIdGroupe, Collectors.counting()));
    }

    /**
     * Données pour PieChart groupes actifs : liste de (nomGroupe, pourcentage).
     */
    public List<Map.Entry<String, Double>> getGroupesActifsPieData() {
        Map<Integer, Long> byGroupe = getMessagesCountByGroupe();
        long total = byGroupe.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0)
            return List.of();

        return byGroupe.entrySet().stream()
                .map(e -> {
                    GroupeChat g = groupeService.getById(e.getKey());
                    String nom = g != null ? g.getNom() : ("Groupe " + e.getKey());
                    double pct = 100.0 * e.getValue() / total;
                    return Map.entry(nom, pct);
                })
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Données pour PieChart équipements les plus vus (nom, nombre_vues).
     * Utilise nombre_vues si disponible, sinon approximation par ordre.
     */
    public List<Map.Entry<String, Number>> getEquipementsPlusVusPieData() {
        List<equipementwael> all = equipementService.afficher();
        if (all.isEmpty())
            return List.of();

        return all.stream()
                .filter(e -> e.getNombreVues() == null || e.getNombreVues() >= 0)
                .sorted((a, b) -> {
                    int va = a.getNombreVues() != null ? a.getNombreVues() : 0;
                    int vb = b.getNombreVues() != null ? b.getNombreVues() : 0;
                    return Integer.compare(vb, va);
                })
                .limit(5)
                .map(e -> Map.entry(e.getNom() != null ? e.getNom() : "?",
                        (Number) (e.getNombreVues() != null ? e.getNombreVues() : 0)))
                .collect(Collectors.toList());
    }
}
