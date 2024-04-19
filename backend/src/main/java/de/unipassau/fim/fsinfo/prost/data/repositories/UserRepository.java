package de.unipassau.fim.fsinfo.prost.data.repositories;

import de.unipassau.fim.fsinfo.prost.data.dao.ProstUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<ProstUser, String> {

}
