# ============================================================
# RefugIA OS — Makefile
# ============================================================

.PHONY: install start index status doctor clean help

help: ## Show this help
	@echo "RefugIA OS — Available commands:"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[33m%-12s\033[0m %s\n", $$1, $$2}'
	@echo ""

install: ## Run the full installer
	./install.sh

start: ## Launch RefugIA server
	./refugia start

index: ## Index PDF survival manuals
	./refugia index

status: ## Show system status
	./refugia status

doctor: ## Diagnose common issues
	./refugia doctor

clean: ## Remove venv and cached data
	@echo "Removing virtual environment..."
	rm -rf venv
	@echo "Removing vector database cache..."
	rm -rf src/db/chroma.sqlite3 src/db/chroma
	@echo "Clean complete. Run 'make install' to reinstall."
