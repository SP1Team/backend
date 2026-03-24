package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.domain.Hall;
import cz.fit.cvut.seatlock.domain.Seat;
import cz.fit.cvut.seatlock.dto.HallDTO;
import cz.fit.cvut.seatlock.repository.ActivityRepository;
import cz.fit.cvut.seatlock.repository.HallRepository;
import cz.fit.cvut.seatlock.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HallService {
    private final HallRepository hallRepository;
    private final SeatRepository seatRepository;
    private final ActivityRepository activityRepository;

    public Hall createHall(HallDTO dto){
        if(hallRepository.existsByName(dto.name())){
            throw new RuntimeException("This name is already occupied");
        }
        Hall newHall = new Hall(dto.name(), dto.address());
        dto.seats().forEach(config -> {
            for(int i = 1; i <= config.countOfSeats(); ++i){
                Seat newSeat = new Seat(config.rowNumber(), i);
                newSeat.setHall(newHall);
                newHall.getSeats().add(newSeat);
            }
        });
        return hallRepository.save(newHall);
    }
}