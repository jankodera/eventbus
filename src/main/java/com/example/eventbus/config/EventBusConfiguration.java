package com.example.eventbus.config;

import com.example.eventbus.domain.SystemWorker;
import com.example.eventbus.domain.repository.SystemWorkerRepository;
import com.example.eventbus.service.IEventBusService;
import com.example.eventbus.service.IIdempotencyService;
import com.example.eventbus.service.IWorkerRegistry;
import com.example.eventbus.worker.consumer.CsvProcessorConsumer;
import com.example.eventbus.worker.producer.FtpDownloadProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventBusConfiguration {

    @Bean
    public FtpDownloadProducer ftpDownloadProducer(SystemWorkerRepository repository,
                                                   IEventBusService eventBusService,
                                                   IIdempotencyService idempotencyService) {
        SystemWorker worker = ensureWorker(repository, "ftp-download-producer",
            FtpDownloadProducer.class.getName(), "Scans FTP directories for new files");
        return new FtpDownloadProducer(worker, eventBusService, idempotencyService);
    }

    @Bean
    public CsvProcessorConsumer csvProcessorConsumer(SystemWorkerRepository repository,
                                                     IEventBusService eventBusService,
                                                     IIdempotencyService idempotencyService,
                                                     ObjectMapper objectMapper) {
        SystemWorker worker = ensureWorker(repository, "csv-processor-consumer",
            CsvProcessorConsumer.class.getName(), "Processes CSV files produced by FTP scans");
        return new CsvProcessorConsumer(worker, eventBusService, idempotencyService, objectMapper);
    }

    @Bean
    public CommandLineRunner workerRegistrationRunner(IWorkerRegistry workerRegistry,
                                                      FtpDownloadProducer ftpDownloadProducer,
                                                      CsvProcessorConsumer csvProcessorConsumer) {
        return args -> {
            workerRegistry.registerProducer(ftpDownloadProducer);
            workerRegistry.registerConsumer(csvProcessorConsumer);
        };
    }

    private SystemWorker ensureWorker(SystemWorkerRepository repository,
                                      String workerName,
                                      String className,
                                      String description) {
        return repository.findByWorkerName(workerName).orElseGet(() -> {
            SystemWorker worker = new SystemWorker();
            worker.setWorkerName(workerName);
            worker.setClassName(className);
            worker.setDescription(description);
            return repository.save(worker);
        });
    }
}
