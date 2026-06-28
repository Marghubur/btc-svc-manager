package bt.conference.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.time.Instant;
import java.util.Arrays;

@Configuration
@EnableMongoRepositories(basePackages = "bt.conference.repository")
@EnableMongoAuditing
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(Arrays.asList(
                new LongToInstantConverter(),
                new InstantToLongConverter()
        ));
    }

    @ReadingConverter
    public static class LongToInstantConverter implements Converter<Long, Instant> {
        @Override
        public Instant convert(Long source) {
            return source == null ? null : Instant.ofEpochMilli(source);
        }
    }

    @WritingConverter
    public static class InstantToLongConverter implements Converter<Instant, Long> {
        @Override
        public Long convert(Instant source) {
            return source == null ? null : source.toEpochMilli();
        }
    }
}