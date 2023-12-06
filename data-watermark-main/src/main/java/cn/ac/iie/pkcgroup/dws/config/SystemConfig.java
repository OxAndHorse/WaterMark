package cn.ac.iie.pkcgroup.dws.config;

import cn.ac.iie.pkcgroup.dws.config.algorithms.db.PatternSearchConf;
import cn.ac.iie.pkcgroup.dws.config.mock.MockAddress;
import cn.ac.iie.pkcgroup.dws.config.mock.MockConfig;
import cn.ac.iie.pkcgroup.dws.config.model.TableConfigs;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class SystemConfig {
    @Value("${conf.path}")
    private Resource config;
    @Value("${conf.algs.patternSearch}")
    private Resource psConf;
    @Value("${conf.mock.mockAddress}")
    private Resource addrConf;
    @Value("${conf.mock.useMock}")
    private int useMock;

    @Bean(value = "mainConfig")
    public TableConfigs initConfig() {
        try {
            String configStr = IOUtils.toString(config.getInputStream(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            return gson.fromJson(configStr, TableConfigs.class);
        } catch (IOException e) {
            log.error("Read config setting error. ", e);
            return null;
        }
    }

    @Bean(value = "patternSearchConf")
    public PatternSearchConf initPatternSearchConf() {
        try {
            String configStr = IOUtils.toString(psConf.getInputStream(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            return gson.fromJson(configStr, PatternSearchConf.class);
        } catch (IOException e) {
            log.error("Read pattern search config error.", e);
            return null;
        }
    }

    @Bean(value = "mockConfig")
    public MockConfig initMockConfig() {
        try {
            MockConfig mockConfig = new MockConfig();
            String configStr = IOUtils.toString(addrConf.getInputStream(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            MockAddress mockAddress = gson.fromJson(configStr, MockAddress.class);
            mockConfig.setUseMock(useMock);
            mockConfig.setMockAddressConfig(mockAddress);
            return mockConfig;
        } catch (IOException e) {
            log.error("Read mock config error.", e);
            return null;
        }
    }
}
