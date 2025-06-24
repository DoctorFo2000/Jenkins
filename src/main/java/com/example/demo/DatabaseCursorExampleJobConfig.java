package com.example.demo;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.jdbc.core.RowMapper;


import org.springframework.core.io.FileSystemResource;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;

import org.springframework.batch.core.launch.support.RunIdIncrementer;


import com.example.model.Employee;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
 
@Configuration
@EnableBatchProcessing
public class DatabaseCursorExampleJobConfig {

	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	

	
	private static final String QUERY_FIND_EMPLOYEES =
          "SELECT " +
             "EMPLOYEE_ID, FIRST_NAME, LAST_NAME, EMAIL, PHONE_NUMBER, hire_date," +
             " job_id, salary, commission_pct, manager_id, department_id " +
          "FROM Employees";
	
	@Value("oracle.jdbc.driver.OracleDriver")
	private String driverClassName;
	
	@Value("jdbc:oracle:thin:@localhost:1521:orcl")
	private String dbUrl;
	
	@Value("hr")
	private String dbUserName;
	
    @Value("hr")
	private String dbPassword;

	@Bean
	DataSource dataSource() {
	    HikariConfig config = new HikariConfig();
	    config.setDriverClassName(driverClassName);
	    config.setJdbcUrl(dbUrl);
	    config.setUsername(dbUserName);
	    config.setPassword(dbPassword);
	    config.setConnectionInitSql(QUERY_FIND_EMPLOYEES);
	    return new HikariDataSource(config);
	}	
	

		
    @Bean
    JdbcCursorItemReader<Employee> reader() {
    	 JdbcCursorItemReader<Employee> reader = new JdbcCursorItemReader<Employee>();
    	 reader.setDataSource(dataSource());
          reader.setSql(QUERY_FIND_EMPLOYEES);
          reader.setRowMapper(new RowMapper<Employee>(){
        	  
        	  @Override
        	  public Employee mapRow(ResultSet rs, int rowNum) throws SQLException{
        		  Employee Employee = new Employee();
        	        Employee.setId(rs.getInt("id"));
        	        Employee.setFirstName(rs.getString("first_name"));
        	        Employee.setLastName(rs.getString("last_name"));
        	        Employee.setEmail(rs.getString("email"));
        	        Employee.setPhone(rs.getInt("phone_number"));
        	        Employee.setHire_date(rs.getDate("hire_date"));
        	        Employee.setJob_id(rs.getString("job_id"));
        	        Employee.setSalary(rs.getFloat("salary"));
        	        Employee.setCommission(rs.getFloat("commission"));
        	        Employee.setManager_id(rs.getInt("manager_id"));
        	        Employee.setDepartment_id(rs.getInt("department_id"));

        	        return Employee;
        		  
        	  }
          });
          return reader;
     
    }
    @Bean
    FlatFileItemWriter <Employee> writer(){
    	FlatFileItemWriter <Employee> writer = new FlatFileItemWriter <Employee> ();
    	writer.setResource(new FileSystemResource("C://Users/User/Desktop/csv-output.csv"));
    	DelimitedLineAggregator<Employee> lineAggregator =
                new DelimitedLineAggregator<>();
    	BeanWrapperFieldExtractor<Employee> extractor =
                new BeanWrapperFieldExtractor<>();
    	extractor.setNames(new String[] {
                "employee_id", "first_name", "last_name", "email",
                "phone_number", "hire_date", "job_id", "salary",
                "commission_pct", "manager_id", "department_id"
        });
    	writer.setLineAggregator(lineAggregator);
    	return writer;
    }
    @Bean
    Step executeStep() {
    	return stepBuilderFactory.get("executeStep").
    			<Employee, Employee>chunk(10)
    	.reader(reader()).
    	writer(writer()).
    	build();
    }
    @Bean
    Job processJob() {
    	return jobBuilderFactory.get("processJob").
    			incrementer(new RunIdIncrementer())
    			.flow(executeStep()).end().build();
    }
}