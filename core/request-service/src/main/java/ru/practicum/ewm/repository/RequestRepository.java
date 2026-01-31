package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.enums.request.RequestStatus;
import ru.practicum.ewm.model.Request;

import java.util.List;
import java.util.Set;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findAllByRequesterId(Long userId);

    List<Request> findAllByEventId(Long eventId);

    List<Request> findAllByIdIn(Set<Long> requestIds);

    boolean existsByEventIdAndRequesterId(Long eventId, Long userId);

    @Modifying
    @Query("UPDATE Request r SET r.status = :status WHERE r.id IN :ids")
    void updateStatus(@Param("status") RequestStatus status, @Param("ids") Set<Long> ids);
}
