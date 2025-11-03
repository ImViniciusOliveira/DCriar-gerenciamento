package com.dcriar.domain.upload.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface que define o contrato para o serviço de armazenamento de arquivos.
 * Abstrai as operações de salvar, carregar e excluir arquivos, permitindo que a implementação
 * seja trocada facilmente (por exemplo, de sistema de arquivos local para um serviço de nuvem como S3).
 */
public interface FileStorageService {

    /**
     * Armazena um arquivo enviado via multipart.
     *
     * @param file O arquivo a ser armazenado.
     * @return O nome único do arquivo gerado após o armazenamento.
     */
    String storeFile(MultipartFile file);

    /**
     * Carrega um arquivo como um recurso (Resource) a partir do seu nome.
     *
     * @param fileName O nome do arquivo a ser carregado.
     * @return Um objeto Resource que representa o arquivo, permitindo sua leitura.
     */
    Resource loadFileAsResource(String fileName);

    /**
     * Exclui um arquivo com base no seu nome.
     *
     * @param fileName O nome do arquivo a ser excluído.
     */
    void deleteFile(String fileName);

    /**
     * Extrai o nome do arquivo a partir de uma URL de download completa.
     *
     * @param fileUrl A URL completa do arquivo.
     * @return O nome do arquivo extraído da URL.
     */
    String extractFileName(String fileUrl);

    /**
     * Retorna a URL completa para acessar um arquivo.
     *
     * @param fileName O nome do arquivo.
     * @return A URL de acesso ao arquivo.
     */
    String getFileUrl(String fileName);

    /**
     * (DEBUG) Lista os nomes de objetos presentes no bucket configurado.
     * Útil para diagnóstico local quando queremos confirmar se um arquivo foi realmente salvo no MinIO.
     *
     * @return array com os nomes de objetos (pode ser grande; usado apenas em dev)
     */
    String[] listAllObjects();

}
