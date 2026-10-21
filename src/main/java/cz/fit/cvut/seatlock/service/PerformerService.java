package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.domain.Performer;
import cz.fit.cvut.seatlock.dto.PerformerPage;
import cz.fit.cvut.seatlock.dto.PerformerRequest;
import cz.fit.cvut.seatlock.es_documents.PerformerDocument;
import cz.fit.cvut.seatlock.es_documents.PerformerSearchRepository;
import cz.fit.cvut.seatlock.events.PerformerEventDTO;
import cz.fit.cvut.seatlock.repository.ActivityRepository;
import cz.fit.cvut.seatlock.repository.PerformerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PerformerService {
    private final PerformerRepository performerRepository;
    private final ActivityRepository activityRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PerformerSearchRepository performerSearchRepository;

    @Transactional
    public Boolean createPerformer(PerformerRequest input) {
        Performer performer = new Performer(input.name(), input.description());
        Performer saved = performerRepository.save(performer);

        eventPublisher.publishEvent(new PerformerEventDTO(
                saved.getId(),
                saved.getName()
        ));
        return true;
    }

    @Transactional
    public Boolean updatePerformer(PerformerRequest request, UUID id) {
        Performer performerToUpdate = performerRepository.findById(id).orElseThrow(() -> new RuntimeException("Performer not found"));

        if(request.name() != null){
            performerToUpdate.setName(request.name());
        }

        if(request.description() != null){
            performerToUpdate.setDescription(request.description());
        }

        performerRepository.save(performerToUpdate);

        eventPublisher.publishEvent(new PerformerEventDTO(
                id,
                performerToUpdate.getName()
        ));
        return true;
    }

    @Transactional
    public Boolean deletePerformer(UUID id) {
        Performer performerToDelete = performerRepository.findById(id).orElseThrow(() -> new RuntimeException("Performer not found"));
        activityRepository.deletePerformerFromActivities(id);
        performerRepository.delete(performerToDelete);
        try {
            performerSearchRepository.deleteById(id.toString());
        } catch (Exception e) {
            log.error("Failed to delete performer from Elasticsearch index for id: {}", id, e);
        }
        return true;
    }

    public PerformerPage getAllPerformers(int page, int size) {
        long total = performerRepository.count();
        List<Performer> content = performerRepository.findAll(page, size);
        int totalPages = (int) Math.ceil((double) total / size);
        return new PerformerPage(content, total, totalPages, page);
    }

    public Performer getPerformerById(UUID id) {
        return performerRepository.findById(id).orElseThrow(() -> new RuntimeException("Performer not found"));
    }

    public List<Performer> search(String text){
        List<PerformerDocument> docs = performerSearchRepository.findByName(text);

        List<UUID> ids = docs.stream().map(doc -> UUID.fromString(doc.getId())).toList();

        if(ids.isEmpty()) return new ArrayList<>();

        return performerRepository.findAllByIds(ids);
    }
}
