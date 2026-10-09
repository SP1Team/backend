package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.domain.Hall;
import cz.fit.cvut.seatlock.domain.Seat;
import cz.fit.cvut.seatlock.dto.HallRequest;
import cz.fit.cvut.seatlock.dto.HallRowResponse;
import cz.fit.cvut.seatlock.dto.HallSeatResponse;
import cz.fit.cvut.seatlock.dto.HallSeatMapResponse;
import cz.fit.cvut.seatlock.repository.HallRepository;
import cz.fit.cvut.seatlock.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HallService {
    private final HallRepository hallRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public Hall createHall(HallRequest request){
        if(hallRepository.findByName(request.name()).isPresent()){
            throw new RuntimeException("This name is already occupied");
        }

        if(hallRepository.findByAddress(request.address()).isPresent()){
            throw new RuntimeException("This address is already occupied");
        }

        Hall newHall = new Hall(request.name(), request.address(), request.city());
        Hall savedHall = hallRepository.save(newHall);

        List<Seat> seatsToSave = new java.util.ArrayList<>();
        request.seats().forEach(config -> {
            for(int i = 1; i <= config.countOfSeats(); ++i){
                Seat newSeat = new Seat(config.rowNumber(), i);
                newSeat.setHall(savedHall);
                seatsToSave.add(newSeat);
                savedHall.getSeats().add(newSeat);
            }
        });

        seatRepository.saveAll(seatsToSave);
        return savedHall;
    }

    public Boolean deleteHall(UUID id) {
        Hall hallToDelete = hallRepository.findById(id).orElseThrow(() -> new RuntimeException("Hall not found"));
        seatRepository.deleteByHallId(id);
        hallRepository.delete(hallToDelete);
        return true;
    }

    public List<Hall> getAllHalls() {
        return hallRepository.findAll();
    }

    public Hall getHallById(UUID id) {
        return hallRepository.findById(id).orElseThrow(() -> new RuntimeException("Hall not found"));
    }

    public HallSeatMapResponse getHallSeatMap(UUID id) {
        hallRepository.findById(id).orElseThrow(() -> new RuntimeException("Hall not found"));

        List<Seat> seats = seatRepository.findByHallId(id);

        Map<Integer, List<HallSeatResponse>> byRow = new LinkedHashMap<>();
        for (Seat seat : seats) {
            byRow.computeIfAbsent(seat.getRowNumber(), k -> new java.util.ArrayList<>())
                    .add(new HallSeatResponse(seat.getId(), seat.getSeatNumber(), seat.getRowNumber()));
        }

        List<HallRowResponse> rows = byRow.entrySet().stream()
                .map(e -> new HallRowResponse(e.getKey(), e.getValue()))
                .toList();

        return new HallSeatMapResponse(rows);
    }

    public Boolean updateHall(HallRequest request, UUID id) {
        Hall hallToUpdate = hallRepository.findById(id).orElseThrow(() -> new RuntimeException("Hall not found"));
        if(request.name() != null){
            hallRepository.findByName(request.name()).ifPresent(found -> {
                if(!found.getId().equals(id)){
                    throw new RuntimeException("Hall with this name already exist");
                }
            });
            hallToUpdate.setName(request.name());
        }

        if(request.address()!= null){
            hallRepository.findByAddress(request.address()).ifPresent(found -> {
                if(!found.getId().equals(id)){
                    throw new RuntimeException("Hall with this address already exist");
                }
            });
            hallToUpdate.setAddress(request.address());
        }

        if(request.city() != null){
            hallToUpdate.setCity(request.city());
        }
        // make seats change in future
        hallRepository.save(hallToUpdate);
        return true;
    }
}
