package repositories;

import java.util.List;
import java.util.Optional;

public interface Repository<T> {
  List<T> findAll();

  Optional<T> findById(int id);

  T save(T entidad);

  boolean update(T entidad);

  boolean delete(int id);
}
