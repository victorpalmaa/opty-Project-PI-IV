# --- IMPORTS ---
import httpx
from bs4 import BeautifulSoup
from typing import List
from urllib.parse import quote_plus
from opty_api.schemas.mercadolivre import MercadoLivreProduct
from fastapi import HTTPException


# --- CODE ---
async def scrape_mercadolivre(query: str) -> List[MercadoLivreProduct]:
    base_url = "https://lista.mercadolivre.com.br/"
    search_url = f"{base_url}{quote_plus(query)}"
    products: List[MercadoLivreProduct] = []

    headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36 Opty-Api Scraper'}

    async with httpx.AsyncClient(headers=headers, follow_redirects=True, timeout=20.0) as client:
        try:
            print(f"\n[DEBUG ML] Buscando por: {search_url}")
            response = await client.get(search_url)
            response.raise_for_status()

            soup = BeautifulSoup(response.content, 'html.parser')

            # Seletor principal (confirmado como funcional)
            item_selector = 'li.ui-search-layout__item'
            items_found = soup.select(item_selector)
            
            print(f"[DEBUG ML] Total de itens encontrados com o seletor '{item_selector}': {len(items_found)}")
            
            # Seletor de Título CONFIRMADO: Tag H3 com as classes
            TITLE_SELECTOR = 'h3.ui-search-item__title.shops__item-title'

            for i, item in enumerate(items_found):
                title, link, final_price, image_url = 'N/A', 'Link não encontrado', 'Preço não encontrado', None
                
                try:
                    # 1. Título (USANDO H3 E CLASSES CONFIRMADAS)
                    title_element = item.select_one(TITLE_SELECTOR)
                    if not title_element:
                         # Fallback para qualquer H3
                         title_element = item.select_one('h3')

                    title = title_element.get_text(strip=True) if title_element else 'Título não encontrado'
                    
                    # 2. Link (BUSCA MAIS SIMPLIFICADA E ABRANGENTE: A primeira tag <a> com href dentro do item)
                    # Isso deve encontrar o link principal, já que ele é o elemento mais proeminente com href.
                    link_element = item.select_one('a[href]')

                    # Extrai o href
                    link = link_element.get('href') if link_element and link_element.get('href') else 'Link não encontrado'
                    
                    # 3. Preço (Lógica que estava funcionando)
                    price_fraction_element = item.select_one('.andes-money-amount__fraction')
                    
                    if price_fraction_element:
                        fraction = price_fraction_element.get_text(strip=True).replace('.', '')
                        cents_element = item.select_one('.andes-money-amount__cents')
                        cents = cents_element.get_text(strip=True) if cents_element else ''
                        
                        final_price = f"R$ {fraction},{cents}" if cents else f"R$ {fraction}"
                        
                        if fraction == '0' and not cents:
                             final_price = 'Preço não encontrado'
                        # 4. Imagem
                        # Tenta pegar a imagem do produto usando seletores mais genéricos
                        img_element = (
                            item.select_one("img.ui-search-result-image__element")  # seletor antigo
                            or item.select_one("img.shops__image-element")          # outro seletor comum
                            or item.select_one("img")                               # fallback genérico
                        )

                        image_url = None
                        if img_element:
                            # Mercado Livre costuma usar lazy-loading com data-src
                            image_url = img_element.get("data-src") or img_element.get("src")

                            # Se vier uma data URI (placeholder 1x1), ignoramos
                            if image_url and image_url.startswith("data:"):
                                image_url = None

                    

                    # Validação final (que estava impedindo a array de encher)
                    if 'não encontrado' not in title and 'não encontrado' not in link and 'não encontrado' not in final_price:
                        products.append(
                            MercadoLivreProduct(
                                title=title,
                                price=final_price,
                                link=link,
                                image=image_url
                            )
                        )
                    
                    # Mantém o debug para o primeiro item
                    if i == 0:
                        print(f"[DEBUG ML - ITEM 1 RESULTADO FINAL] Título: {title}")
                        print(f"[DEBUG ML - ITEM 1 RESULTADO FINAL] Link: {link}")
                        print(f"[DEBUG ML - ITEM 1 RESULTADO FINAL] Preço: {final_price}")


                except Exception as e:
                    if i == 0:
                        print(f"[DEBUG ML - ITEM 1 ERRO GERAL] {e}")
                    continue
            
            if products:
                print(f"[DEBUG ML - SUCESSO COMPLETO] {len(products)} itens extraídos com sucesso.")
            
            return products

        # Captura de erros HTTP e de Conexão
        except httpx.HTTPStatusError as e:
            raise HTTPException(status_code=503, detail=f"Erro ao acessar Mercado Livre: {e.response.status_code}")
        except httpx.RequestError as e:
            raise HTTPException(status_code=504, detail="Erro de conexão ou timeout ao acessar Mercado Livre.")
        except Exception as e:
            print(f"Erro inesperado no scraping: {e}")
            raise HTTPException(status_code=500, detail="Erro interno ao processar dados de scraping.")