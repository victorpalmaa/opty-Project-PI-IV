// front-end/src/pages/Resultados.tsx

import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { AlertTriangle, ChevronRight, SlidersHorizontal, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
// Importar api e useToast
import { api } from '@/services/api';
import { useToast } from '@/components/ui/use-toast';
import { Checkbox } from '@/components/ui/checkbox';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Label } from '@/components/ui/label';
import { Slider } from '@/components/ui/slider';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet';
import DashboardNav from '@/components/DashboardNav';
import ProductCard from '@/components/ProductCard';
import Footer from '@/components/Footer';

// --- TIPAGEM DO BACKEND ---
interface MercadoLivreProduct {
  title: string;
  price: string; // Ex: "R$ 1.549,65"
  link: string;
  image?: string;
  source: string; // "Mercado Livre"
}

// --- TIPAGEM DO FRONT-END (MOCK) ---
interface Offer {
  store: string;
  price: number;
  condition: string;
  shipping: string;
  link: string;
}

interface ProductDisplay {
  name: string;
  image?: string;
  offers: Offer[];
}

const Resultados = () => {
  const [searchParams] = useSearchParams();
  const searchQuery = searchParams.get('q') || 'Produto não especificado';

  // Estados reais para dados e controle da UI
  const [products, setProducts] = useState<ProductDisplay[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { toast } = useToast();

  // Estados de filtros
  const [filters, setFilters] = useState({
    priceMin: 0,
    priceMax: 10000,
    stores: [] as string[],
    condition: 'all',
  });
  const [sortBy, setSortBy] = useState('lowest-price');
  const [showMobileFilters, setShowMobileFilters] = useState(false);

  const stores = [
    { id: 'mercadolivre', name: 'Mercado Livre' },
    // Outras lojas estarão aqui quando forem implementadas no backend
    { id: 'amazon', name: 'Amazon' },
    { id: 'magalu', name: 'Magazine Luiza' },
  ];

  // --- FUNÇÃO DE BUSCA REAL ---
  const fetchResults = async (query: string) => {
    if (!query) return;

    setIsLoading(true);
    setError(null);

    try {
      const mlProducts: MercadoLivreProduct[] = await api.search.mercadolivre(query);

      // Mapeamento e Transformação dos Produtos do Backend para o formato do Card
      const mappedProducts: ProductDisplay[] = mlProducts.map((p) => {
        // Remove R$ e pontos de milhar, troca vírgula por ponto para conversão em float
        const numericPrice = parseFloat(
          p.price.replace('R$', '').replace(/\./g, '').replace(',', '.').trim()
        );

        const offer: Offer = {
          store: 'Mercado Livre',
          price: isNaN(numericPrice) ? 0 : numericPrice,
          condition: 'Novo', // Simplificação da condição
          shipping: 'Grátis', // Simplificação do frete
          link: p.link,
        };

        return {
          name: p.title,
          image: p.image,
          offers: [offer], // Cada produto do ML é sua única oferta por enquanto
        };
      });

      setProducts(mappedProducts);
    } catch (err) {
      console.error('Erro na busca: ', err);
      const errorMessage =
        (err as any).response?.data?.detail || 'Erro desconhecido ao comunicar com o servidor.';
      setError(`Falha na busca. Detalhe: ${errorMessage}`);
      toast({
        title: 'Erro de Busca',
        description: 'Não foi possível carregar os resultados do Mercado Livre.',
        variant: 'destructive',
      });
    } finally {
      setIsLoading(false);
    }
  };

  // Efeito para buscar os dados na montagem do componente ou na mudança da query
  useEffect(() => {
    fetchResults(searchQuery);
  }, [searchQuery]);

  // --- FUNÇÕES DE FILTRO ---

  const toggleStore = (storeId: string) => {
    setFilters((prev) => ({
      ...prev,
      stores: prev.stores.includes(storeId)
        ? prev.stores.filter((id) => id !== storeId)
        : [...prev.stores, storeId],
    }));
  };

  const clearFilters = () => {
    setFilters({
      priceMin: 0,
      priceMax: 10000,
      stores: [],
      condition: 'all',
    });
  };

  const FilterContent = () => (
    <div className='space-y-6'>
      {/* Clear Filters */}
      <div className='flex items-center justify-between'>
        <h3 className='font-semibold text-lg'>Filtros</h3>
        <Button variant='ghost' size='sm' onClick={clearFilters}>
          Limpar
        </Button>
      </div>

      {/* Price Range */}
      <div>
        <Label className='mb-3 block font-semibold'>Faixa de Preço</Label>
        <div className='space-y-4'>
          <Slider
            min={0}
            max={10000}
            step={100}
            value={[filters.priceMin, filters.priceMax]}
            onValueChange={([min, max]) =>
              setFilters({ ...filters, priceMin: min, priceMax: max })
            }
            className='mb-4'
          />
          <div className='flex gap-2'>
            <div className='flex-1'>
              <Label className='text-xs text-muted-foreground mb-1 block'>Mínimo</Label>
              <Input
                type='number'
                value={filters.priceMin}
                onChange={(e) =>
                  setFilters({ ...filters, priceMin: Number(e.target.value) || 0 })
                }
                className='h-9'
              />
            </div>
            <div className='flex-1'>
              <Label className='text-xs text-muted-foreground mb-1 block'>Máximo</Label>
              <Input
                type='number'
                value={filters.priceMax}
                onChange={(e) =>
                  setFilters({ ...filters, priceMax: Number(e.target.value) || 0 })
                }
                className='h-9'
              />
            </div>
          </div>
        </div>
      </div>

      {/* Stores Filter */}
      <div>
        <Label className='mb-3 block font-semibold'>
          Lojas {filters.stores.length > 0 && `(${filters.stores.length})`}
        </Label>
        <div className='space-y-2'>
          {stores.map((store) => (
            <div key={store.id} className='flex items-center space-x-2'>
              <Checkbox
                id={store.id}
                checked={filters.stores.includes(store.id)}
                onCheckedChange={() => toggleStore(store.id)}
              />
              <Label htmlFor={store.id} className='cursor-pointer font-normal'>
                {store.name}
              </Label>
            </div>
          ))}
        </div>
      </div>

      {/* Condition Filter */}
      <div>
        <Label className='mb-3 block font-semibold'>Condição</Label>
        <RadioGroup
          value={filters.condition}
          onValueChange={(val) => setFilters({ ...filters, condition: val })}
        >
          <div className='flex items-center space-x-2'>
            <RadioGroupItem value='all' id='all' />
            <Label htmlFor='all' className='cursor-pointer font-normal'>
              Todos
            </Label>
          </div>
          <div className='flex items-center space-x-2'>
            <RadioGroupItem value='new' id='new' />
            <Label htmlFor='new' className='cursor-pointer font-normal'>
              Novo
            </Label>
          </div>
          <div className='flex items-center space-x-2'>
            <RadioGroupItem value='used' id='used' />
            <Label htmlFor='used' className='cursor-pointer font-normal'>
              Seminovo/Usado
            </Label>
          </div>
        </RadioGroup>
      </div>
    </div>
  );

  // =========================================================
  //  AQUI ENTRA O “PASSO 1”: FILTRAR E ORDENAR PRODUTOS
  // =========================================================

    // 1) Filtra ofertas por preço + condição
  const productsWithinPriceRange = products
    .map((product) => {
      const offersFiltered = product.offers.filter((offer) => {
        const price = offer.price ?? 0;

        // Preço dentro da faixa
        const priceOk = price >= filters.priceMin && price <= filters.priceMax;

        // Normaliza condição do anúncio
        const cond = offer.condition.toLowerCase();
        const isNew = cond.includes('novo');
        const isUsed =
          cond.includes('usado') ||
          cond.includes('seminovo') ||
          cond.includes('recondicionado');

        let conditionOk = true;
        if (filters.condition === 'new') {
          conditionOk = isNew;
        } else if (filters.condition === 'used') {
          conditionOk = isUsed;
        }

        return priceOk && conditionOk;
      });

      return {
        ...product,
        offers: offersFiltered,
      };
    })
    // mantém só produtos que ainda têm pelo menos uma oferta após os filtros
    .filter((product) => product.offers.length > 0);


  // 2) Ordena de acordo com o "Ordenar por"
  const sortedProducts = [...productsWithinPriceRange];

  if (sortBy === 'lowest-price' || sortBy === 'highest-price') {
    sortedProducts.sort((a, b) => {
      const minPriceA = Math.min(...a.offers.map((o) => o.price));
      const minPriceB = Math.min(...b.offers.map((o) => o.price));
      return minPriceA - minPriceB;
    });

    if (sortBy === 'highest-price') {
      sortedProducts.reverse();
    }
  } else if (sortBy === 'alphabetical') {
    sortedProducts.sort((a, b) => a.name.localeCompare(b.name));
  }

  // --- RETURN DO COMPONENTE ---
  return (
    <div className='min-h-screen flex flex-col bg-background'>
      <DashboardNav userName='João' />

      {/* Breadcrumb */}
      <div className='border-b border-border bg-muted/30'>
        <div className='container mx-auto px-4 py-3'>
          <div className='flex items-center gap-2 text-sm'>
            <Link to='/dashboard' className='text-muted-foreground hover:text-primary'>
              Dashboard
            </Link>
            <ChevronRight className='h-4 w-4 text-muted-foreground' />
            <span className='text-foreground font-medium'>
              Resultados para '{searchQuery}'
            </span>
          </div>
        </div>
      </div>

      <div className='flex-1 container mx-auto px-4 py-6'>
        <div className='flex flex-col lg:flex-row gap-6'>
          {/* Filters Sidebar - Desktop */}
          <aside className='hidden lg:block w-80 flex-shrink-0'>
            <div className='sticky top-24 glass p-6 rounded-xl'>
              <FilterContent />
            </div>
          </aside>

          {/* Main Content */}
          <main className='flex-1'>
            {/* Header */}
            <div className='mb-6'>
              <div className='flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-4'>
                <div>
                  <h1 className='text-2xl sm:text-3xl font-bold text-foreground mb-2'>
                    Resultados para:{' '}
                    <span className='gradient-text'>{searchQuery}</span>
                  </h1>
                  <p className='text-muted-foreground'>
                    {isLoading
                      ? 'Buscando produtos...'
                      : `Encontramos ${sortedProducts.length} produtos`}
                  </p>
                </div>

                {/* Mobile Filters Button */}
                <Sheet open={showMobileFilters} onOpenChange={setShowMobileFilters}>
                  <SheetTrigger asChild>
                    <Button variant='outline' className='lg:hidden'>
                      <SlidersHorizontal className='h-4 w-4 mr-2' />
                      Filtros
                    </Button>
                  </SheetTrigger>
                  <SheetContent side='left' className='w-80'>
                    <SheetHeader>
                      <SheetTitle>Filtros de Busca</SheetTitle>
                    </SheetHeader>
                    <div className='mt-6'>
                      <FilterContent />
                    </div>
                  </SheetContent>
                </Sheet>
              </div>

              {/* Sort */}
              <div className='flex items-center gap-4'>
                <Label className='text-sm font-medium'>Ordenar por:</Label>
                <Select value={sortBy} onValueChange={setSortBy}>
                  <SelectTrigger className='w-[200px]'>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value='lowest-price'>Menor Preço</SelectItem>
                    <SelectItem value='highest-price'>Maior Preço</SelectItem>
                    <SelectItem value='best-rating'>Melhor Avaliação</SelectItem>
                    <SelectItem value='most-sold'>Mais Vendidos</SelectItem>
                    <SelectItem value='alphabetical'>
                      Ordem Alfabética (A-Z)
                    </SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* --- DISPLAY DE ESTADOS --- */}

            {/* Loading State */}
            {isLoading && (
              <div className='flex justify-center items-center h-64'>
                <Loader2 className='h-8 w-8 animate-spin text-primary' />
                <p className='ml-3 text-lg text-muted-foreground'>
                  Carregando os melhores preços...
                </p>
              </div>
            )}

            {/* Error State */}
            {error && !isLoading && (
              <div className='text-center py-12 glass p-6 rounded-xl border border-red-400'>
                <AlertTriangle className='h-10 w-10 text-red-600 mx-auto mb-4' />
                <h3 className='text-xl font-semibold mb-2 text-red-700'>
                  Erro na Busca
                </h3>
                <p className='text-red-600 mb-4'>{error}</p>
              </div>
            )}

            {/* Products Grid (Show only if not loading and no error) */}
            {!isLoading && !error && (
              <div className='space-y-4'>
                {sortedProducts.length > 0 ? (
                  sortedProducts.map((product, index) => (
                    <ProductCard
                      key={index}
                      name={product.name}
                      image={product.image}
                      offers={product.offers}
                    />
                  ))
                ) : (
                  // Empty State
                  <div className='text-center py-12'>
                    <div className='text-6xl mb-4'>🔍</div>
                    <h3 className='text-xl font-semibold mb-2'>
                      Nenhum resultado encontrado
                    </h3>
                    <p className='text-muted-foreground mb-4'>
                      Nenhuma oferta foi encontrada para "{searchQuery}". Tente
                      uma busca mais geral ou cheque a ortografia.
                    </p>
                    <Button
                      variant='gradient'
                      onClick={() => (window.location.href = '/dashboard')}
                    >
                      Voltar ao Dashboard
                    </Button>
                  </div>
                )}
              </div>
            )}

            {/* --- FIM DISPLAY DE ESTADOS --- */}
          </main>
        </div>
      </div>

      <Footer />
    </div>
  );
};

export default Resultados;
