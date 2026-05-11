import com.example.dao.InMemoryWorldDao;
import com.example.dao.WorldDao;
import com.example.domain.City;
import com.example.domain.Country;
import com.example.domain.Director;
import com.example.domain.Movie;
import com.example.exercises.ContinentCityPair;
import com.example.exercises.DirectorGenrePair;
import com.example.service.InMemoryMovieService;
import com.example.service.MovieService;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class SolutionTest {

    private final MovieService movieService = InMemoryMovieService.getInstance();

    private final WorldDao worldDao = InMemoryWorldDao.getInstance();

    //Q1: Find the number of movies of each director
    @Test
    public void solutionQ1(){
        var result = movieService.findAllMovies().
                stream()
                .flatMap(movie -> movie.getDirectors().stream()).collect(groupingBy(Director::getName,Collectors.counting()));

        System.out.print(result);
    }

    //Q2:Find the most populated city of each continent
    @Test
    public void solutionQ2(){
        Comparator<ContinentCityPair> comparator = Comparator.comparing(continentCityPair -> continentCityPair.city().getPopulation());
        var result = worldDao.findAllCountries()
                .stream()
                .flatMap(country -> country.getCities().stream().map(city -> new ContinentCityPair(country.getContinent(),city)))
                .collect(groupingBy(ContinentCityPair::continent,Collectors.maxBy(comparator)));

        System.out.println(result);
    }

    //Q3:Find the number of genres of each director's movies
    @Test
    public void solutionQ3(){
        var result = movieService
                .findAllMovies()
                .stream()
                .flatMap(movie -> movie.getDirectors().stream().flatMap(director -> movie.getGenres().stream().map(genre -> new DirectorGenrePair(director,genre))))
                .collect(groupingBy(pair -> pair.director().getName(),groupingBy(pair -> pair.genre().getName(),Collectors.counting())));

        System.out.println(result);
    }

    //Q4:Find the highest populated capital city
    @Test
    public void solutionQ4(){
        var result = worldDao.findAllCountries()
                .stream()
                .map(country -> country.getCities().stream().filter(city -> city.getId() == country.getCapital()).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .max(Comparator.comparing(City::getPopulation));

        System.out.println(result);
    }

    //Q5:Find the highest populated capital city of each continent
    @Test
    public void solutionQ5(){
        var result = worldDao.findAllCountries()
                .stream()
                .flatMap(country -> country.getCities().stream().filter(city -> city.getId() == country.getCapital()).map(city -> new ContinentCityPair(country.getContinent(),city)))
                .collect(groupingBy(ContinentCityPair::continent,maxBy(Comparator.comparing(pair -> pair.city().getPopulation()))));

        System.out.println(result);
    }

    //Q6: Sort the countries by number of their cities in descending order
    @Test
    public void solutionQ6() {
        var result = worldDao
                .findAllCountries()
                .stream()
                .sorted(Comparator.<Country,Integer>comparing(country -> country.getCities().size()).reversed())
                .toList();

        System.out.println(result);
    }

    //Q7:Find the list of movies having the genres "Drama" and "Comedy" only
    @Test
    public void solutionQ7(){
        var result = movieService
                .findAllMovies()
                .stream()
                .filter(movie -> movie.getGenres().size() == 2)
                .filter(movie -> movie.getGenres().stream().filter(genre -> genre.getName().equals("Drama") || genre.getName().equals("Comedy")).toList().size() == 2)
                .toList();

        System.out.println(result);
    }

    //Q8:Group the movies by the year and list them
    @Test
    public void solutionQ8(){
        var result = movieService
                .findAllMovies()
                .stream()
                .collect(groupingBy(Movie::getYear));

        System.out.println(result);
    }


    //Q9:Sort the countries by their population densities in descending order ignoring zero population countries
    @Test
    public void solution9(){
        var result = worldDao
                .findAllCountries()
                .stream()
                .filter(country -> country.getPopulation() > 0)
                .sorted(Comparator.<Country>comparingDouble(country -> country.getPopulation()/country.getSurfaceArea()).reversed())
                .peek(country -> System.out.println(country.getName()+" = "+country.getPopulation()/country.getSurfaceArea()))
                .toList();

        System.out.println(result);

    }

    //Q10: Find the richest country of each continent with respect to their GNP (Gross National Product) values.???
    @Test
    public void solution10(){
        var result = worldDao
                .findAllCountries()
                .stream()
                .collect(groupingBy(Country::getContinent,maxBy(Comparator.comparing(Country::getGnp))));

        System.out.println(result);
    }

    //Q11:Find the minimum, the maximum and the average population of world countries.
    @Test
    public void solution11(){
        var result = worldDao
                .findAllCountries()
                .stream()
                .collect(Collectors.summarizingLong(Country::getPopulation));

        System.out.println(result);
    }

    //Q12:Find the minimum, the maximum and the average population of each continent.
    @Test
    public void solution12(){
        var result = worldDao
                .findAllCountries()
                .stream()
                .collect(groupingBy(Country::getContinent,summarizingLong(Country::getPopulation)));

        System.out.println(result);
    }

    private static class CountryStatistics implements Collector<Country,CountryStatistics,CountryStatistics> {

        private Optional<Country> min = Optional.empty();
        private Optional<Country> max = Optional.empty();

        public CountryStatistics(){

        }

        private CountryStatistics(Optional<Country> min, Optional<Country> max){
            this.min = min;
            this.max = max;
        }

        public void newCountry(Country country) {
            min = min.map(min -> min.getPopulation() > country.getPopulation() ? country : min).or(() -> Optional.of(country));
            max = max.map(max -> max.getPopulation() < country.getPopulation() ? country : max).or(() -> Optional.of(country));
        }

        public CountryStatistics combineWith(CountryStatistics other) {

            this.min = min.map( currentMin -> currentMin.getPopulation() <= other.min.map(Country::getPopulation).orElse(currentMin.getPopulation()) ? currentMin : other.min.orElse(currentMin));
            this.max = max.map( currentMax -> currentMax.getPopulation() <= other.max.map(Country::getPopulation).orElse(currentMax.getPopulation()) ? currentMax : other.max.orElse(currentMax));

            return this;
        }

        public Optional<Country> getMin() {
            return min;
        }

        public Optional<Country> getMax() {
            return max;
        }

        @Override
        public Supplier<CountryStatistics> supplier() {
            return CountryStatistics::new;
        }

        @Override
        public BiConsumer<CountryStatistics, Country> accumulator() {
            return CountryStatistics::newCountry;
        }

        @Override
        public BinaryOperator<CountryStatistics> combiner() {
            return CountryStatistics::combineWith;
        }

        @Override
        public Function<CountryStatistics, CountryStatistics> finisher() {
            return Function.identity();
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }

        @Override
        public String toString() {
            return "CountryStatistics{" +
                    "min=" + min +
                    ", max=" + max +
                    '}';
        }
    }

    //Q13:Find the countries with the minimum and the maximum population.
    @Test
    public void solution13(){
        var result = worldDao
                .findAllCountries()
                .stream()
                .filter(country -> country.getPopulation() > 0)
                .collect(new CountryStatistics());

        System.out.println(result);
    }

    //Q14:Find the countries of each continent with the minimum and the maximum population.
    @Test
    public void solution14(){
        var result = worldDao
                .findAllCountries()
                .stream()
                .filter(country -> country.getPopulation() > 0)
                .collect(groupingBy(Country::getContinent,new CountryStatistics()));

        System.out.println(result);
    }

    //Q15:Group the countries by continent, and then sort the countries in continent by number of cities in each continent.
    @Test
    public void solution15(){
        var result = worldDao
                .findAllCountries()
                .stream()
                .sorted(Comparator.<Country>comparingInt(country -> country.getCities().size()).reversed())
                .collect(groupingBy(Country::getContinent));

        System.out.println(result);
    }

    //Q16:Find the cities with the minimum and the maximum population in countries.
    @Test
    public void solution16(){
        record CityCountryPair(City city,Country country) {}

        class CityStatistics implements Collector<CityCountryPair,CityStatistics,CityStatistics> {

            private Optional<City> min = Optional.empty();
            private Optional<City> max = Optional.empty();

            public Optional<City> getMin() {
                return min;
            }

            public Optional<City> getMax() {
                return max;
            }

            @Override
            public String toString() {
                return "CityStatistics{" +
                        "min=" + min +
                        ", max=" + max +
                        '}';
            }

            private void newCity(CityCountryPair pair){
                min = min.map( min -> min.getPopulation() < pair.city.getPopulation()? min : pair.city).or( () -> Optional.of(pair.city));
                max = max.map( max -> max.getPopulation() > pair.city.getPopulation()? max : pair.city).or( () -> Optional.of(pair.city));
            }

            private CityStatistics combineWith(CityStatistics other) {
                this.min = min.map( currentMin -> currentMin.getPopulation() <= other.min.map(City::getPopulation).orElse(currentMin.getPopulation()) ? currentMin : other.min.orElse(currentMin));
                this.max = max.map( currentMax -> currentMax.getPopulation() <= other.max.map(City::getPopulation).orElse(currentMax.getPopulation()) ? currentMax : other.max.orElse(currentMax));

                return this;
            }

            @Override
            public Supplier<CityStatistics> supplier() {
                return CityStatistics::new;
            }

            @Override
            public BiConsumer<CityStatistics, CityCountryPair> accumulator() {
                return CityStatistics::newCity;
            }

            @Override
            public BinaryOperator<CityStatistics> combiner() {
                return CityStatistics::combineWith;
            }

            @Override
            public Function<CityStatistics, CityStatistics> finisher() {
                return Function.identity();
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Set.of();
            }
        }

        var result = worldDao
                .findAllCountries()
                .stream()
                .flatMap(country -> country.getCities().stream().map(city -> new CityCountryPair(city, country)))
                .collect(groupingBy(CityCountryPair::country,new CityStatistics()));

        System.out.println(result);
    }

    //Q18:Find the year where the maximum number of movie is available
    @Test
    public void solution18(){
        var result = movieService
                .findAllMovies()
                .stream()
                .collect(groupingBy(Movie::getYear,counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue());

        System.out.println(result);

    }

}
