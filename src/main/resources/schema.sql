DROP TABLE IF EXISTS transactions;

CREATE TABLE transactions (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              card_number BIGINT NOT NULL,
                              security_code BIGINT NOT NULL ,
                              amount NUMERIC NOT NULL,
                              state VARCHAR(25) NOT NULL ,
                              comment VARCHAR(256),
                              transaction_time TIMESTAMP NOT NULL
);
