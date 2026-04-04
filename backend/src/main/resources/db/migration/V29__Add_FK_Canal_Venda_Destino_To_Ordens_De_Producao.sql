ALTER TABLE ordens_de_producao
    ADD CONSTRAINT fk_ordem_producao_canal_venda_destino
        FOREIGN KEY (canal_venda_destino_id) REFERENCES canais_venda(id);
