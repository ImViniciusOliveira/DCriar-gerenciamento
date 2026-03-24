package com.dcriar.exception.custom;

import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Exceção lançada quando uma tentativa de excluir uma Ordem de Produção é bloqueada
 * por regras de integridade (ex: produtos já vendidos ou retalhos já utilizados).
 * <p>
 * Esta exceção garante que o sistema não permita operações que deixariam o estoque
 * em estado inconsistente ou negativo.
 */
@Getter
public class ImpossivelExcluirProducaoException extends RuntimeException {

    /**
     * Constrói a exceção com a mensagem detalhando o motivo do bloqueio.
     *
     * @param mensagem A descrição do erro e o motivo pelo qual a exclusão não é permitida.
     */
    private ImpossivelExcluirProducaoException(String mensagem) {
        super(mensagem);
    }

    public static ImpossivelExcluirProducaoException estoqueInsuficienteParaEstorno(
            int quantidadeProduzida,
            int saldoAtualProduto
    ) {
        return new ImpossivelExcluirProducaoException(String.format(
                "Estoque insuficiente para estorno. Produzido: %d, Saldo Atual: %d. Produtos já foram vendidos ou consumidos.",
                quantidadeProduzida,
                saldoAtualProduto
        ));
    }

    public static ImpossivelExcluirProducaoException retalhoJaUtilizado(ContextoRetalhoBloqueado contexto) {
        return new ImpossivelExcluirProducaoException(
                String.format(
                        "Não é possível excluir a Ordem de Produção #%d porque a árvore de retalhos ainda possui alteração ativa. " +
                                "Retalho em conflito: Lote #%d, gerado pela Ordem de Produção #%d. " +
                                "Cadeia da árvore: %s. " +
                                "Ordens relacionadas: %s. " +
                                "Motivo do bloqueio: %s.",
                        contexto.ordemDeProducaoId(),
                        contexto.loteId(),
                        contexto.ordemDeProducaoOrigemId(),
                        formatarCadeiaRetalhos(contexto.cadeiaRetalhos()),
                        formatarOrdensRelacionadas(contexto.ordensRelacionadasIds()),
                        formatarDetalheUsoAtivo(contexto.tipoAlteracaoAtiva(), contexto.ordemConsumidoraAtivaId())
                )
        );
    }

    public static ImpossivelExcluirProducaoException estoqueCanalInsuficienteParaEstorno(
            Long canalVendaId,
            int quantidadeProduzida,
            int estoqueAtualCanal
    ) {
        return new ImpossivelExcluirProducaoException(String.format(
                "Estoque insuficiente no canal #%d para estorno da produção. Produzido: %d, Estoque no Canal: %d.",
                canalVendaId,
                quantidadeProduzida,
                estoqueAtualCanal
        ));
    }

    private static String formatarCadeiaRetalhos(List<CadeiaRetalhoItem> cadeiaRetalhos) {
        return cadeiaRetalhos.stream()
                .map(item -> item.ordemDeProducaoOrigemId() == null
                        ? String.format("Lote raiz #%d", item.loteId())
                        : String.format("Retalho #%d (OP #%d)", item.loteId(), item.ordemDeProducaoOrigemId()))
                .collect(Collectors.joining(" -> "));
    }

    private static String formatarOrdensRelacionadas(List<Long> ordensRelacionadasIds) {
        return ordensRelacionadasIds.stream()
                .map(id -> "OP #" + id)
                .collect(Collectors.joining(", "));
    }

    private static String formatarDetalheUsoAtivo(TipoMovimentacao tipoAlteracaoAtiva, Long ordemConsumidoraAtivaId) {
        if (tipoAlteracaoAtiva == null) {
            return "o saldo atual do retalho ainda difere do saldo originalmente gerado";
        }

        return switch (tipoAlteracaoAtiva) {
            case SAIDA_PRODUCAO -> ordemConsumidoraAtivaId != null
                    ? String.format("consumo ainda ativo pela Ordem de Produção #%d", ordemConsumidoraAtivaId)
                    : "consumo ainda ativo no retalho";
            case PERDA_DESCARTE -> "há perda/descarte ativo registrado no retalho";
            case AJUSTE_INVENTARIO -> "há ajuste de inventário ativo registrado no retalho";
            default -> "há alteração ativa registrada no retalho";
        };
    }

    public record ContextoRetalhoBloqueado(
            Long ordemDeProducaoId,
            Long loteId,
            Long ordemDeProducaoOrigemId,
            List<CadeiaRetalhoItem> cadeiaRetalhos,
            List<Long> ordensRelacionadasIds,
            TipoMovimentacao tipoAlteracaoAtiva,
            Long ordemConsumidoraAtivaId
    ) {
    }

    public record CadeiaRetalhoItem(
            Long loteId,
            Long ordemDeProducaoOrigemId
    ) {
    }
}
