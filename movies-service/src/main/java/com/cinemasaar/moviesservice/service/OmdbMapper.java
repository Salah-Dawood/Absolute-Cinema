package com.cinemasaar.moviesservice.service;

import org.springframework.stereotype.Component;
import com.cinemasaar.moviesservice.model.Movie;
import com.cinemasaar.moviesservice.dto.OmdbMovieResponse;
import java.math.BigDecimal;

// Applies external metadata while preserving local identity, showing status and creation time.
@Component
public class OmdbMapper {
    public void apply(OmdbMovieResponse source, Movie target) {
        target.setTitle(clean(source.getTitle()));
        target.setYear(clean(source.getYear()));
        target.setRated(clean(source.getRated()));
        target.setReleased(clean(source.getReleased()));
        target.setRuntime(clean(source.getRuntime()));
        target.setGenre(clean(source.getGenre()));
        target.setDirector(clean(source.getDirector()));
        target.setWriter(clean(source.getWriter()));
        target.setActors(clean(source.getActors()));
        target.setPlot(clean(source.getPlot()));
        target.setLanguage(clean(source.getLanguage()));
        target.setCountry(clean(source.getCountry()));
        target.setAwards(clean(source.getAwards()));
        target.setBoxOffice(clean(source.getBoxOffice()));
        target.setType(clean(source.getType()));
        target.setDvd(clean(source.getDvd()));
        target.setProduction(clean(source.getProduction()));
        target.setImdbId(clean(source.getImdbId()));
        target.setPosterUrl(clean(source.getPoster())); target.setWebsiteUrl(clean(source.getWebsite()));
        target.setImdbRating(decimal(source.getImdbRating())); target.setImdbVotes(integer(source.getImdbVotes()));
        target.setMetascore(integer(source.getMetascore())); target.setTotalSeasons(integer(source.getTotalSeasons()));
    }
    private String clean(String value) { return value == null || value.isBlank() || "N/A".equalsIgnoreCase(value.trim()) ? null : value.trim(); }
    private Integer integer(String value) {
        try { String cleaned = clean(value); return cleaned == null ? null : Integer.valueOf(cleaned.replace(",", "")); }
        catch (NumberFormatException e) { return null; }
    }
    private BigDecimal decimal(String value) {
        try { String cleaned = clean(value); return cleaned == null ? null : new BigDecimal(cleaned); }
        catch (NumberFormatException e) { return null; }
    }
}
